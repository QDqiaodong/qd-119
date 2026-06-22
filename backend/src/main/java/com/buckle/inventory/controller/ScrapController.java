package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.dto.ScrapRequest;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.service.ScrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scrap")
@CrossOrigin
public class ScrapController {

    @Autowired
    private ScrapService scrapService;

    @GetMapping
    public Result<PageResult<ScrapRecord>> listScrap(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(scrapService.listScrap(page, size));
    }

    @PostMapping
    public Result<ScrapRecord> addScrap(@RequestBody ScrapRequest request) {
        return Result.ok(scrapService.addScrap(request));
    }

    @GetMapping("/{id}")
    public Result<ScrapRecord> getById(@PathVariable Long id) {
        return Result.ok(scrapService.getById(id));
    }
}
