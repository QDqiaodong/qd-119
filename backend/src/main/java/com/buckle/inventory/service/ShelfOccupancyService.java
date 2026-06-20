package com.buckle.inventory.service;

import com.buckle.inventory.dto.ShelfOccupancyInfo;

public interface ShelfOccupancyService {

    ShelfOccupancyInfo getShelfOccupancy(String shelfPosition);

    void checkCapacity(String shelfPosition, int additionalQuantity, boolean isNewPartType);

    int getMaxPartTypes();

    int getMaxStockCapacity();
}
