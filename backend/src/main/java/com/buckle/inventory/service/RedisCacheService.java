package com.buckle.inventory.service;

import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.dto.DashboardOverview;
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

    List<BucklePartDTO> getBucklesFromCache();

    void setBucklesCache(List<BucklePartDTO> buckles);

    List<BracketPartDTO> getBracketsFromCache();

    void setBracketsCache(List<BracketPartDTO> brackets);

    DashboardOverview getDashboardOverviewFromCache();

    void setDashboardOverviewCache(DashboardOverview overview);

    void evictBucklesCache();

    void evictBracketsCache();

    void evictDashboardOverviewCache();

    void evictAllInventoryRelatedCache();

    Long getOutboundIdempotentRecord(String key);

    void setOutboundIdempotentRecord(String key, Long recordId);
}
