package com.buckle.inventory.controller;

import com.buckle.inventory.dto.InboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.service.InboundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound")
@CrossOrigin
public class InboundController {

    @Autowired
    private InboundService inboundService;

    @GetMapping
    public Result<PageResult<InboundRecord>> listInbound(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(inboundService.listInbound(page, size, keyword));
    }

    @PostMapping
    public Result<InboundRecord> addInbound(@RequestBody InboundRequest request) {
        return Result.ok(inboundService.addInbound(request));
    }
}
