package com.buckle.inventory.controller;

import com.buckle.inventory.dto.OutboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.service.OutboundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outbound")
@CrossOrigin
public class OutboundController {

    @Autowired
    private OutboundService outboundService;

    @GetMapping
    public Result<PageResult<OutboundRecord>> listOutbound(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productionLine,
            @RequestParam(required = false) Long machineId) {
        return Result.ok(outboundService.listOutbound(page, size, productionLine, machineId));
    }

    @PostMapping
    public Result<OutboundRecord> addOutbound(@RequestBody OutboundRequest request) {
        return Result.ok(outboundService.addOutbound(request));
    }
}
