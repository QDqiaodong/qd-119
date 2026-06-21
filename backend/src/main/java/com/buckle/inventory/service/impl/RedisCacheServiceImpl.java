package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.dto.DashboardOverview;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private static final String PARTS_CACHE_KEY = "parts:common";
    private static final String PARTS_SHELF_PREFIX = "parts:shelf:";
    private static final String PARTS_CATEGORY_PREFIX = "parts:category:";
    private static final String BUCKLES_CACHE_KEY = "parts:buckles";
    private static final String BRACKETS_CACHE_KEY = "parts:brackets";
    private static final String DASHBOARD_OVERVIEW_KEY = "dashboard:overview";
    private static final long CACHE_TTL_MINUTES = 30;

    private void probe(String action, String key, Object value) {
        log.info("[CACHE_PROBE] action={} key={} value={}", action, key, value);
    }

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
        probe("KEY_GEN", cacheKey, "shelf=" + shelfPosition);
        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            probe("READ_ERROR", cacheKey, e.getMessage());
        }
        if (cached != null) {
            try {
                List<Part> result = objectMapper.readValue(cached.toString(), new TypeReference<List<Part>>() {});
                probe("HIT", cacheKey, result.size());
                return result;
            } catch (Exception e) {
                probe("DESERIALIZE_FAIL", cacheKey, e.getMessage());
                try { redisTemplate.delete(cacheKey); } catch (Exception ignored) {}
            }
        }
        probe("MISS", cacheKey, "LOAD_FROM_DB");
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1).eq(Part::getShelfPosition, shelfPosition);
        List<Part> parts = partMapper.selectList(wrapper);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(parts),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            probe("WRITE", cacheKey, parts.size() + "|TTL=" + CACHE_TTL_MINUTES + "m");
        } catch (Exception e) {
            probe("WRITE_FAIL", cacheKey, e.getMessage());
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
        probe("KEY_GEN", cacheKey, "categoryId=" + categoryId);
        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            probe("READ_ERROR", cacheKey, e.getMessage());
        }
        if (cached != null) {
            try {
                List<Part> result = objectMapper.readValue(cached.toString(), new TypeReference<List<Part>>() {});
                probe("HIT", cacheKey, result.size());
                return result;
            } catch (Exception e) {
                probe("DESERIALIZE_FAIL", cacheKey, e.getMessage());
                try { redisTemplate.delete(cacheKey); } catch (Exception ignored) {}
            }
        }
        probe("MISS", cacheKey, "LOAD_FROM_DB");
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1).eq(Part::getCategoryId, categoryId);
        List<Part> parts = partMapper.selectList(wrapper);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(parts),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            probe("WRITE", cacheKey, parts.size() + "|TTL=" + CACHE_TTL_MINUTES + "m");
        } catch (Exception e) {
            probe("WRITE_FAIL", cacheKey, e.getMessage());
            throw new RuntimeException("缓存类别配件失败", e);
        }
        return parts;
    }

    @Override
    public void evictPartsByShelfPosition(String shelfPosition) {
        if (StringUtils.hasText(shelfPosition)) {
            String key = PARTS_SHELF_PREFIX + shelfPosition;
            try {
                redisTemplate.delete(key);
                probe("EVICT", key, "OK");
            } catch (Exception e) {
                probe("EVICT_FAIL", key, e.getMessage());
            }
        }
        evictPartsCache();
    }

    @Override
    public void evictPartsByCategoryId(Long categoryId) {
        if (categoryId != null) {
            String key = PARTS_CATEGORY_PREFIX + categoryId;
            try {
                redisTemplate.delete(key);
                probe("EVICT", key, "OK");
            } catch (Exception e) {
                probe("EVICT_FAIL", key, e.getMessage());
            }
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
        probe("EVICT_BATCH_START", "partId=" + partId,
                "oldShelf=" + oldShelfPosition + ",oldCat=" + oldCategoryId
                        + ",newShelf=" + newShelfPosition + ",newCat=" + newCategoryId);
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
        evictAllInventoryRelatedCache();
        probe("EVICT_BATCH_END", "partId=" + partId, "DONE");
    }

    @Override
    public List<BucklePartDTO> getBucklesFromCache() {
        Object cached = redisTemplate.opsForValue().get(BUCKLES_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), new TypeReference<List<BucklePartDTO>>() {});
            } catch (Exception e) {
                redisTemplate.delete(BUCKLES_CACHE_KEY);
            }
        }
        return null;
    }

    @Override
    public void setBucklesCache(List<BucklePartDTO> buckles) {
        try {
            redisTemplate.opsForValue().set(BUCKLES_CACHE_KEY, objectMapper.writeValueAsString(buckles),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存卡扣列表失败", e);
        }
    }

    @Override
    public List<BracketPartDTO> getBracketsFromCache() {
        Object cached = redisTemplate.opsForValue().get(BRACKETS_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), new TypeReference<List<BracketPartDTO>>() {});
            } catch (Exception e) {
                redisTemplate.delete(BRACKETS_CACHE_KEY);
            }
        }
        return null;
    }

    @Override
    public void setBracketsCache(List<BracketPartDTO> brackets) {
        try {
            redisTemplate.opsForValue().set(BRACKETS_CACHE_KEY, objectMapper.writeValueAsString(brackets),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存支架列表失败", e);
        }
    }

    @Override
    public DashboardOverview getDashboardOverviewFromCache() {
        Object cached = redisTemplate.opsForValue().get(DASHBOARD_OVERVIEW_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), DashboardOverview.class);
            } catch (Exception e) {
                redisTemplate.delete(DASHBOARD_OVERVIEW_KEY);
            }
        }
        return null;
    }

    @Override
    public void setDashboardOverviewCache(DashboardOverview overview) {
        try {
            redisTemplate.opsForValue().set(DASHBOARD_OVERVIEW_KEY, objectMapper.writeValueAsString(overview),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存仪表盘概览失败", e);
        }
    }

    @Override
    public void evictBucklesCache() {
        redisTemplate.delete(BUCKLES_CACHE_KEY);
    }

    @Override
    public void evictBracketsCache() {
        redisTemplate.delete(BRACKETS_CACHE_KEY);
    }

    @Override
    public void evictDashboardOverviewCache() {
        redisTemplate.delete(DASHBOARD_OVERVIEW_KEY);
    }

    @Override
    public void evictAllInventoryRelatedCache() {
        evictPartsCache();
        evictBucklesCache();
        evictBracketsCache();
        evictDashboardOverviewCache();
    }

    private List<Part> refreshAndReturn() {
        refreshPartsCache();
        return getPartsFromCache();
    }
}
