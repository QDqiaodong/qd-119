package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.dto.ShelfMigrationRequest;
import com.buckle.inventory.entity.ShelfMigrationRecord;
import com.buckle.inventory.service.ShelfMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shelf-migration")
@CrossOrigin
public class ShelfMigrationController {

    @Autowired
    private ShelfMigrationService shelfMigrationService;

    @GetMapping
    public Result<PageResult<ShelfMigrationRecord>> listMigrationRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(shelfMigrationService.listMigrationRecords(page, size, keyword));
    }

    @PostMapping
    public Result<ShelfMigrationRecord> addShelfMigration(@RequestBody ShelfMigrationRequest request) {
        return Result.ok(shelfMigrationService.addShelfMigration(request));
    }
}
