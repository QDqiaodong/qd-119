package com.buckle.inventory.service;

import com.buckle.inventory.entity.Part;

import java.util.List;

public interface RedisCacheService {

    void refreshPartsCache();

    List<Part> getPartsFromCache();

    void evictPartsCache();

    List<Part> getPartsByShelfPosition(String shelfPosition);

    List<Part> getPartsByCategoryId(Long categoryId);

    void evictPartsByShelfPosition(String shelfPosition);

    void evictPartsByCategoryId(Long categoryId);

    void evictPartRelatedCache(Part part);

    void evictPartRelatedCache(Long partId, String oldShelfPosition, Long oldCategoryId,
                               String newShelfPosition, Long newCategoryId);
}
