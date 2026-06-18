package com.buckle.inventory.service;

import com.buckle.inventory.entity.Part;

import java.util.List;

public interface RedisCacheService {

    void refreshPartsCache();

    List<Part> getPartsFromCache();

    void evictPartsCache();
}
