package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageInboundRequest;
import com.buckle.inventory.entity.BucklePackage;
import com.buckle.inventory.entity.BucklePackageItem;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.PackageInboundDetail;
import com.buckle.inventory.entity.PackageInboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.BucklePackageItemMapper;
import com.buckle.inventory.mapper.BucklePackageMapper;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.PackageInboundDetailMapper;
import com.buckle.inventory.mapper.PackageInboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.PackageInboundService;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ShelfOccupancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageInboundServiceImpl implements PackageInboundService {

    @Autowired
    private PackageInboundRecordMapper packageInboundRecordMapper;

    @Autowired
    private PackageInboundDetailMapper packageInboundDetailMapper;

    @Autowired
    private BucklePackageMapper bucklePackageMapper;

    @Autowired
    private BucklePackageItemMapper bucklePackageItemMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ShelfOccupancyService shelfOccupancyService;

    @Override
    public PageResult<PackageInboundRecord> listInbound(int page, int size, String keyword) {
        LambdaQueryWrapper<PackageInboundRecord> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<BucklePackage> matchedPackages = bucklePackageMapper.selectList(
                    new LambdaQueryWrapper<BucklePackage>()
                            .like(BucklePackage::getName, keyword)
                            .or()
                            .like(BucklePackage::getCode, keyword));
            if (!matchedPackages.isEmpty()) {
                List<Long> packageIds = matchedPackages.stream().map(BucklePackage::getId).collect(Collectors.toList());
                queryWrapper.and(w -> w.in(PackageInboundRecord::getPackageId, packageIds)
                        .or().like(PackageInboundRecord::getPackageName, keyword)
                        .or().like(PackageInboundRecord::getPackageCode, keyword));
            } else {
                queryWrapper.and(w -> w.like(PackageInboundRecord::getPackageName, keyword)
                        .or().like(PackageInboundRecord::getPackageCode, keyword));
            }
        }

        queryWrapper.orderByDesc(PackageInboundRecord::getCreatedAt);
        Page<PackageInboundRecord> pageParam = new Page<>(page, size);
        Page<PackageInboundRecord> result = packageInboundRecordMapper.selectPage(pageParam, queryWrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PackageInboundRecord getInboundById(Long id) {
        PackageInboundRecord record = packageInboundRecordMapper.selectById(id);
        if (record != null) {
            List<PackageInboundDetail> details = packageInboundDetailMapper.selectByRecordId(id);
            record.setDetails(details);
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageInboundRecord addInbound(PackageInboundRequest request) {
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        int packageQuantity = request.getPackageQuantity();

        BucklePackage pkg = bucklePackageMapper.selectById(request.getPackageId());
        if (pkg == null) {
            throw new RuntimeException("成套包不存在");
        }
        if (pkg.getStatus() == null || pkg.getStatus() != 1) {
            throw new RuntimeException("成套包已停用，无法入库");
        }

        List<BucklePackageItem> packageItems = bucklePackageItemMapper.selectByPackageIdWithPartInfo(request.getPackageId());
        if (packageItems == null || packageItems.isEmpty()) {
            throw new RuntimeException("成套包中没有配件，无法入库");
        }

        for (BucklePackageItem item : packageItems) {
            Part part = partMapper.selectById(item.getPartId());
            if (part == null) {
                throw new RuntimeException("配件不存在，ID: " + item.getPartId());
            }
            if (part.getDeleted() != null && part.getDeleted() == 1) {
                throw new RuntimeException("配件已删除: " + part.getName());
            }

            int actualQuantity = item.getQuantity() * packageQuantity;

            shelfOccupancyService.checkCapacity(part.getShelfPosition(), actualQuantity, false);

            int affected = partMapper.addStock(item.getPartId(), actualQuantity, now);
            if (affected == 0) {
                throw new RuntimeException("更新配件库存失败: " + part.getName());
            }

            InboundRecord inboundRecord = new InboundRecord();
            inboundRecord.setPartId(item.getPartId());
            inboundRecord.setQuantity(actualQuantity);
            inboundRecord.setShelfPosition(part.getShelfPosition());
            inboundRecord.setOperator(request.getOperator() != null ? request.getOperator().trim() : "system");
            inboundRecord.setCreatedAt(now);
            inboundRecord.setPartName(part.getName());
            inboundRecord.setPartModel(part.getModel());
            inboundRecordMapper.insert(inboundRecord);

            redisCacheService.evictPartRelatedCache(part);
        }

        PackageInboundRecord record = new PackageInboundRecord();
        record.setPackageId(pkg.getId());
        record.setPackageName(pkg.getName());
        record.setPackageCode(pkg.getCode());
        record.setPackageQuantity(packageQuantity);
        record.setOperator(request.getOperator() != null ? request.getOperator().trim() : "system");
        record.setRemark(request.getRemark() != null ? request.getRemark().trim() : null);
        record.setCreatedAt(now);
        packageInboundRecordMapper.insert(record);

        if (record.getId() == null) {
            throw new RuntimeException("记录成套包入库流水失败");
        }

        for (BucklePackageItem item : packageItems) {
            Part part = partMapper.selectById(item.getPartId());
            int actualQuantity = item.getQuantity() * packageQuantity;

            PackageInboundDetail detail = new PackageInboundDetail();
            detail.setRecordId(record.getId());
            detail.setPartId(item.getPartId());
            detail.setPartName(part.getName());
            detail.setPartModel(part.getModel());
            detail.setQuantity(actualQuantity);
            detail.setShelfPosition(part.getShelfPosition());
            packageInboundDetailMapper.insert(detail);
        }

        redisCacheService.evictAllInventoryRelatedCache();
        return getInboundById(record.getId());
    }

    private void validateRequest(PackageInboundRequest request) {
        if (request == null) {
            throw new RuntimeException("入库请求不能为空");
        }
        if (request.getPackageId() == null) {
            throw new RuntimeException("成套包ID不能为空");
        }
        if (request.getPackageQuantity() == null) {
            throw new RuntimeException("成套包数量不能为空");
        }
        if (request.getPackageQuantity() <= 0) {
            throw new RuntimeException("成套包数量必须大于0，当前值: " + request.getPackageQuantity());
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
    }
}
