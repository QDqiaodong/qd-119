package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageOutboundRequest;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.PackageOutboundRecord;
import com.buckle.inventory.service.PackageOutboundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/package-outbound")
@CrossOrigin
public class PackageOutboundController {

    @Autowired
    private PackageOutboundService packageOutboundService;

    @GetMapping
    public Result<PageResult<PackageOutboundRecord>> listOutbound(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) Long machineId) {
        return Result.ok(packageOutboundService.listOutbound(page, size, productionLine, machineId));
    }

    @GetMapping("/{id}")
    public Result<PackageOutboundRecord> getOutboundById(@PathVariable Long id) {
        return Result.ok(packageOutboundService.getOutboundById(id));
    }

    @PostMapping
    public Result<PackageOutboundRecord> addOutbound(@RequestBody PackageOutboundRequest request) {
        return Result.ok(packageOutboundService.addOutbound(request));
    }
}
