package com.buckle.inventory.controller;

import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.service.AccessoryCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accessory-categories")
@CrossOrigin
public class AccessoryCategoryController {

    @Autowired
    private AccessoryCategoryService categoryService;

    @GetMapping
    public Result<List<AccessoryCategory>> listAll() {
        return Result.ok(categoryService.listAll());
    }

    @GetMapping("/{id}")
    public Result<AccessoryCategory> getById(@PathVariable Long id) {
        return Result.ok(categoryService.getById(id));
    }

    @PostMapping
    public Result<AccessoryCategory> add(@RequestBody AccessoryCategory category) {
        return Result.ok(categoryService.add(category));
    }

    @PutMapping("/{id}")
    public Result<AccessoryCategory> update(@PathVariable Long id, @RequestBody AccessoryCategory category) {
        category.setId(id);
        return Result.ok(categoryService.update(category));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage(), null);
        }
        return Result.ok(null);
    }
}
