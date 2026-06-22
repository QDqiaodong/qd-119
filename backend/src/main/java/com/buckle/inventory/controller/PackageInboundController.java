package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageInboundRequest;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.PackageInboundRecord;
import com.buckle.inventory.service.PackageInboundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/package-inbound")
@CrossOrigin
public class PackageInboundController {

    @Autowired
    private PackageInboundService packageInboundService;

    @GetMapping
    public Result<PageResult<PackageInboundRecord>> listInbound(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(packageInboundService.listInbound(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<PackageInboundRecord> getInboundById(@PathVariable Long id) {
        return Result.ok(packageInboundService.getInboundById(id));
    }

    @PostMapping
    public Result<PackageInboundRecord> addInbound(@RequestBody PackageInboundRequest request) {
        return Result.ok(packageInboundService.addInbound(request));
    }
}
