package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageRequest;
import com.buckle.inventory.entity.BucklePackage;

import java.util.List;

public interface BucklePackageService {

    PageResult<BucklePackage> listPackages(int page, int size, String keyword, Integer status);

    BucklePackage getPackageById(Long id);

    BucklePackage getPackageDetail(Long id);

    List<BucklePackage> getAllEnabledPackages();

    BucklePackage createPackage(PackageRequest request);

    BucklePackage updatePackage(Long id, PackageRequest request);

    void deletePackage(Long id);
}
