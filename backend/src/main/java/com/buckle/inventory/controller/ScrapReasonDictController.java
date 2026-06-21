package com.buckle.inventory.controller;

import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.ScrapReasonDict;
import com.buckle.inventory.service.ScrapReasonDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scrap-reasons")
@CrossOrigin
public class ScrapReasonDictController {

    @Autowired
    private ScrapReasonDictService scrapReasonDictService;

    @GetMapping("/enabled")
    public Result<List<ScrapReasonDict>> listEnabled() {
        return Result.ok(scrapReasonDictService.listEnabled());
    }

    @GetMapping
    public Result<List<ScrapReasonDict>> listAll() {
        return Result.ok(scrapReasonDictService.listAll());
    }

    @GetMapping("/{id}")
    public Result<ScrapReasonDict> getById(@PathVariable Long id) {
        return Result.ok(scrapReasonDictService.getById(id));
    }

    @GetMapping("/code/{code}")
    public Result<ScrapReasonDict> getByCode(@PathVariable String code) {
        return Result.ok(scrapReasonDictService.getByCode(code));
    }

    @PostMapping
    public Result<ScrapReasonDict> add(@RequestBody ScrapReasonDict dict) {
        return Result.ok(scrapReasonDictService.add(dict));
    }

    @PutMapping("/{id}")
    public Result<ScrapReasonDict> update(@PathVariable Long id, @RequestBody ScrapReasonDict dict) {
        dict.setId(id);
        return Result.ok(scrapReasonDictService.update(dict));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scrapReasonDictService.delete(id);
        return Result.ok(null);
    }
}
