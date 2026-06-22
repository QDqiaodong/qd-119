package com.buckle.inventory.controller;

import com.buckle.inventory.dto.InventoryCheckRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.InventoryCheck;
import com.buckle.inventory.service.InventoryCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin
public class InventoryCheckController {

    @Autowired
    private InventoryCheckService inventoryCheckService;

    @GetMapping
    public Result<PageResult<InventoryCheck>> listChecks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(inventoryCheckService.listChecks(page, size));
    }

    @PostMapping
    public Result<InventoryCheck> addCheck(@RequestBody InventoryCheckRequest request) {
        return Result.ok(inventoryCheckService.addCheck(request));
    }

    @GetMapping("/{id}")
    public Result<InventoryCheck> getCheckDetail(@PathVariable Long id) {
        return Result.ok(inventoryCheckService.getCheckDetail(id));
    }

    @GetMapping("/quarters")
    public Result<java.util.List<String>> listAvailableQuarters() {
        return Result.ok(inventoryCheckService.listAvailableQuarters());
    }

    @PostMapping("/{id}/complete")
    public Result<InventoryCheck> completeCheck(@PathVariable Long id) {
        return Result.ok(inventoryCheckService.completeCheck(id));
    }

    @GetMapping("/quarters/{quarter}/locked")
    public Result<Boolean> isQuarterLocked(@PathVariable String quarter) {
        return Result.ok(inventoryCheckService.isQuarterLocked(quarter));
    }
}
