package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.ShelfMigrationRequest;
import com.buckle.inventory.entity.ShelfMigrationRecord;

public interface ShelfMigrationService {

    PageResult<ShelfMigrationRecord> listMigrationRecords(int page, int size, String keyword);

    ShelfMigrationRecord addShelfMigration(ShelfMigrationRequest request);
}
