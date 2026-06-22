package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageRequest;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.BucklePackage;
import com.buckle.inventory.service.BucklePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buckle-packages")
@CrossOrigin
public class BucklePackageController {

    @Autowired
    private BucklePackageService bucklePackageService;

    @GetMapping
    public Result<PageResult<BucklePackage>> listPackages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(bucklePackageService.listPackages(page, size, keyword, status));
    }

    @GetMapping("/enabled")
    public Result<List<BucklePackage>> getAllEnabledPackages() {
        return Result.ok(bucklePackageService.getAllEnabledPackages());
    }

    @GetMapping("/{id}")
    public Result<BucklePackage> getPackageById(@PathVariable Long id) {
        return Result.ok(bucklePackageService.getPackageById(id));
    }

    @GetMapping("/{id}/detail")
    public Result<BucklePackage> getPackageDetail(@PathVariable Long id) {
        return Result.ok(bucklePackageService.getPackageDetail(id));
    }

    @PostMapping
    public Result<BucklePackage> createPackage(@RequestBody PackageRequest request) {
        return Result.ok(bucklePackageService.createPackage(request));
    }

    @PutMapping("/{id}")
    public Result<BucklePackage> updatePackage(@PathVariable Long id, @RequestBody PackageRequest request) {
        return Result.ok(bucklePackageService.updatePackage(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePackage(@PathVariable Long id) {
        bucklePackageService.deletePackage(id);
        return Result.ok(null);
    }
}
