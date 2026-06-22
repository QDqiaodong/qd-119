package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageItemRequest;
import com.buckle.inventory.dto.PackageRequest;
import com.buckle.inventory.entity.BucklePackage;
import com.buckle.inventory.entity.BucklePackageItem;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.BucklePackageItemMapper;
import com.buckle.inventory.mapper.BucklePackageMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.BucklePackageService;
import com.buckle.inventory.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BucklePackageServiceImpl implements BucklePackageService {

    @Autowired
    private BucklePackageMapper packageMapper;

    @Autowired
    private BucklePackageItemMapper packageItemMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<BucklePackage> listPackages(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<BucklePackage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BucklePackage::getName, keyword)
                    .or().like(BucklePackage::getCode, keyword));
        }
        if (status != null) {
            wrapper.eq(BucklePackage::getStatus, status);
        }
        wrapper.orderByAsc(BucklePackage::getSortOrder).orderByDesc(BucklePackage::getCreatedAt);

        Page<BucklePackage> pageParam = new Page<>(page, size);
        Page<BucklePackage> result = packageMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public BucklePackage getPackageById(Long id) {
        return packageMapper.selectById(id);
    }

    @Override
    public BucklePackage getPackageDetail(Long id) {
        BucklePackage pkg = packageMapper.selectById(id);
        if (pkg != null) {
            List<BucklePackageItem> items = packageItemMapper.selectByPackageIdWithPartInfo(id);
            pkg.setItems(items);
        }
        return pkg;
    }

    @Override
    public List<BucklePackage> getAllEnabledPackages() {
        LambdaQueryWrapper<BucklePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BucklePackage::getStatus, 1);
        wrapper.orderByAsc(BucklePackage::getSortOrder).orderByDesc(BucklePackage::getCreatedAt);
        List<BucklePackage> packages = packageMapper.selectList(wrapper);
        for (BucklePackage pkg : packages) {
            List<BucklePackageItem> items = packageItemMapper.selectByPackageIdWithPartInfo(pkg.getId());
            pkg.setItems(items);
        }
        return packages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BucklePackage createPackage(PackageRequest request) {
        validatePackageRequest(request);

        LocalDateTime now = LocalDateTime.now();
        BucklePackage pkg = new BucklePackage();
        pkg.setName(request.getName().trim());
        pkg.setCode(request.getCode().trim());
        pkg.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        pkg.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        pkg.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        pkg.setCreatedAt(now);
        pkg.setUpdatedAt(now);

        packageMapper.insert(pkg);
        if (pkg.getId() == null) {
            throw new RuntimeException("创建成套包失败");
        }

        savePackageItems(pkg.getId(), request.getItems(), now);

        redisCacheService.evictAllInventoryRelatedCache();
        return getPackageDetail(pkg.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BucklePackage updatePackage(Long id, PackageRequest request) {
        BucklePackage existing = packageMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("成套包不存在");
        }

        validatePackageRequest(request, id);

        LocalDateTime now = LocalDateTime.now();
        existing.setName(request.getName().trim());
        existing.setCode(request.getCode().trim());
        existing.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        existing.setStatus(request.getStatus() != null ? request.getStatus() : existing.getStatus());
        existing.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : existing.getSortOrder());
        existing.setUpdatedAt(now);

        packageMapper.updateById(existing);

        packageItemMapper.deleteByPackageId(id);
        savePackageItems(id, request.getItems(), now);

        redisCacheService.evictAllInventoryRelatedCache();
        return getPackageDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long id) {
        BucklePackage existing = packageMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("成套包不存在");
        }

        packageItemMapper.deleteByPackageId(id);
        packageMapper.deleteById(id);

        redisCacheService.evictAllInventoryRelatedCache();
    }

    private void validatePackageRequest(PackageRequest request) {
        validatePackageRequest(request, null);
    }

    private void validatePackageRequest(PackageRequest request, Long excludeId) {
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("成套包名称不能为空");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new RuntimeException("成套包编码不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("请至少添加一个卡扣型号");
        }

        LambdaQueryWrapper<BucklePackage> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(BucklePackage::getCode, request.getCode().trim());
        if (excludeId != null) {
            codeWrapper.ne(BucklePackage::getId, excludeId);
        }
        Long count = packageMapper.selectCount(codeWrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("成套包编码已存在");
        }

        for (PackageItemRequest item : request.getItems()) {
            if (item.getPartId() == null) {
                throw new RuntimeException("配件ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("配件数量必须大于0");
            }
            Part part = partMapper.selectById(item.getPartId());
            if (part == null) {
                throw new RuntimeException("配件不存在，ID: " + item.getPartId());
            }
            if (part.getDeleted() != null && part.getDeleted() == 1) {
                throw new RuntimeException("配件已删除: " + part.getName());
            }
        }

        List<Long> partIds = request.getItems().stream()
                .map(PackageItemRequest::getPartId)
                .collect(Collectors.toList());
        if (partIds.size() != partIds.stream().distinct().count()) {
            throw new RuntimeException("不能添加重复的配件");
        }
    }

    private void savePackageItems(Long packageId, List<PackageItemRequest> items, LocalDateTime now) {
        if (items == null) {
            return;
        }
        for (PackageItemRequest item : items) {
            BucklePackageItem packageItem = new BucklePackageItem();
            packageItem.setPackageId(packageId);
            packageItem.setPartId(item.getPartId());
            packageItem.setQuantity(item.getQuantity());
            packageItem.setCreatedAt(now);
            packageItemMapper.insert(packageItem);
        }
    }
}
