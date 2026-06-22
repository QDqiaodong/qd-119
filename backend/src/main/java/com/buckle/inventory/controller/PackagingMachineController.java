package com.buckle.inventory.controller;

import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.PackagingMachine;
import com.buckle.inventory.service.PackagingMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packaging-machines")
@CrossOrigin
public class PackagingMachineController {

    @Autowired
    private PackagingMachineService packagingMachineService;

    @GetMapping
    public Result<List<PackagingMachine>> list(
            @RequestParam(required = false) String productionLine) {
        if (productionLine != null && !productionLine.isEmpty()) {
            return Result.ok(packagingMachineService.listByProductionLine(productionLine));
        }
        return Result.ok(packagingMachineService.listAll());
    }

    @GetMapping("/{id}")
    public Result<PackagingMachine> getById(@PathVariable Long id) {
        return Result.ok(packagingMachineService.getById(id));
    }

    @GetMapping("/code/{code}")
    public Result<PackagingMachine> getByCode(@PathVariable String code) {
        return Result.ok(packagingMachineService.getByCode(code));
    }
}
