package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final String PARTS_CACHE_KEY = "parts:common";
    private static final String PARTS_SHELF_PREFIX = "parts:shelf:";
    private static final String PARTS_CATEGORY_PREFIX = "parts:category:";
    private static final long CACHE_TTL_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void refreshPartsCache() {
        evictPartsCache();
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1);
        List<Part> parts = partMapper.selectList(wrapper);
        for (Part part : parts) {
            try {
                String json = objectMapper.writeValueAsString(part);
                redisTemplate.opsForList().rightPush(PARTS_CACHE_KEY, json);
            } catch (Exception e) {
                throw new RuntimeException("刷新Redis缓存失败", e);
            }
        }
        redisTemplate.expire(PARTS_CACHE_KEY, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public List<Part> getPartsFromCache() {
        List<Object> jsonList = redisTemplate.opsForList().range(PARTS_CACHE_KEY, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) {
            return refreshAndReturn();
        }
        List<Part> parts = new ArrayList<>();
        for (Object json : jsonList) {
            try {
                Part part = objectMapper.readValue(json.toString(), Part.class);
                parts.add(part);
            } catch (Exception e) {
                return refreshAndReturn();
            }
        }
        return parts;
    }

    @Override
    public void evictPartsCache() {
        redisTemplate.delete(PARTS_CACHE_KEY);
    }

    @Override
    public List<Part> getPartsByShelfPosition(String shelfPosition) {
        if (!StringUtils.hasText(shelfPosition)) {
            return new ArrayList<>();
        }
        String cacheKey = PARTS_SHELF_PREFIX + shelfPosition;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), new TypeReference<List<Part>>() {});
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1).eq(Part::getShelfPosition, shelfPosition);
        List<Part> parts = partMapper.selectList(wrapper);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(parts),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存货架配件失败", e);
        }
        return parts;
    }

    @Override
    public List<Part> getPartsByCategoryId(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        String cacheKey = PARTS_CATEGORY_PREFIX + categoryId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), new TypeReference<List<Part>>() {});
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1).eq(Part::getCategoryId, categoryId);
        List<Part> parts = partMapper.selectList(wrapper);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(parts),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存类别配件失败", e);
        }
        return parts;
    }

    @Override
    public void evictPartsByShelfPosition(String shelfPosition) {
        if (StringUtils.hasText(shelfPosition)) {
            redisTemplate.delete(PARTS_SHELF_PREFIX + shelfPosition);
        }
        evictPartsCache();
    }

    @Override
    public void evictPartsByCategoryId(Long categoryId) {
        if (categoryId != null) {
            redisTemplate.delete(PARTS_CATEGORY_PREFIX + categoryId);
        }
        evictPartsCache();
    }

    @Override
    public void evictPartRelatedCache(Part part) {
        if (part == null) {
            return;
        }
        evictPartRelatedCache(part.getId(), part.getShelfPosition(), part.getCategoryId(),
                part.getShelfPosition(), part.getCategoryId());
    }

    @Override
    public void evictPartRelatedCache(Long partId, String oldShelfPosition, Long oldCategoryId,
                                      String newShelfPosition, Long newCategoryId) {
        if (StringUtils.hasText(oldShelfPosition)) {
            evictPartsByShelfPosition(oldShelfPosition);
        }
        if (StringUtils.hasText(newShelfPosition) && !newShelfPosition.equals(oldShelfPosition)) {
            evictPartsByShelfPosition(newShelfPosition);
        }
        if (oldCategoryId != null) {
            evictPartsByCategoryId(oldCategoryId);
        }
        if (newCategoryId != null && !newCategoryId.equals(oldCategoryId)) {
            evictPartsByCategoryId(newCategoryId);
        }
        evictPartsCache();
    }

    private List<Part> refreshAndReturn() {
        refreshPartsCache();
        return getPartsFromCache();
    }
}
