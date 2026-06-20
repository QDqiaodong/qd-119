package com.buckle.inventory.service.impl;

import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final String PARTS_CACHE_KEY = "parts:common";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void refreshPartsCache() {
        evictPartsCache();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Part> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
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
        redisTemplate.expire(PARTS_CACHE_KEY, 30, TimeUnit.MINUTES);
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

    private List<Part> refreshAndReturn() {
        refreshPartsCache();
        return getPartsFromCache();
    }
}
