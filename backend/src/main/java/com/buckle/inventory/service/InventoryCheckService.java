package com.buckle.inventory.service;

import com.buckle.inventory.dto.InventoryCheckRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InventoryCheck;

public interface InventoryCheckService {

    PageResult<InventoryCheck> listChecks(int page, int size);

    InventoryCheck addCheck(InventoryCheckRequest request);

    InventoryCheck getCheckDetail(Long id);

    java.util.List<String> listAvailableQuarters();

    String normalizeQuarter(String quarter);

    InventoryCheck completeCheck(Long id);

    boolean isQuarterLocked(String quarter);
}
