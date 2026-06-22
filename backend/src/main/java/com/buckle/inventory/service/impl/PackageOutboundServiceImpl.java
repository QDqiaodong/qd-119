package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageOutboundRequest;
import com.buckle.inventory.entity.BucklePackage;
import com.buckle.inventory.entity.BucklePackageItem;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.PackageOutboundDetail;
import com.buckle.inventory.entity.PackageOutboundRecord;
import com.buckle.inventory.entity.PackagingMachine;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.BucklePackageItemMapper;
import com.buckle.inventory.mapper.BucklePackageMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PackageOutboundDetailMapper;
import com.buckle.inventory.mapper.PackageOutboundRecordMapper;
import com.buckle.inventory.mapper.PackagingMachineMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.PackageOutboundService;
import com.buckle.inventory.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PackageOutboundServiceImpl implements PackageOutboundService {

    private static final Logger log = LoggerFactory.getLogger(PackageOutboundServiceImpl.class);

    @Autowired
    private PackageOutboundRecordMapper packageOutboundRecordMapper;

    @Autowired
    private PackageOutboundDetailMapper packageOutboundDetailMapper;

    @Autowired
    private BucklePackageMapper bucklePackageMapper;

    @Autowired
    private BucklePackageItemMapper bucklePackageItemMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private PackagingMachineMapper packagingMachineMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<PackageOutboundRecord> listOutbound(int page, int size, String productionLine, Long machineId) {
        Page<PackageOutboundRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PackageOutboundRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productionLine)) {
            wrapper.eq(PackageOutboundRecord::getProductionLine, productionLine);
        }
        if (machineId != null) {
            wrapper.eq(PackageOutboundRecord::getMachineId, machineId);
        }
        wrapper.orderByDesc(PackageOutboundRecord::getCreatedAt);
        Page<PackageOutboundRecord> result = packageOutboundRecordMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PackageOutboundRecord getOutboundById(Long id) {
        PackageOutboundRecord record = packageOutboundRecordMapper.selectById(id);
        if (record != null) {
            List<PackageOutboundDetail> details = packageOutboundDetailMapper.selectByRecordId(id);
            record.setDetails(details);
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageOutboundRecord addOutbound(PackageOutboundRequest request) {
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        int packageQuantity = request.getPackageQuantity();

        BucklePackage pkg = bucklePackageMapper.selectById(request.getPackageId());
        if (pkg == null) {
            throw new RuntimeException("成套包不存在");
        }
        if (pkg.getStatus() == null || pkg.getStatus() != 1) {
            throw new RuntimeException("成套包已停用，无法出库");
        }

        List<BucklePackageItem> packageItems = bucklePackageItemMapper.selectByPackageIdWithPartInfo(request.getPackageId());
        if (packageItems == null || packageItems.isEmpty()) {
            throw new RuntimeException("成套包中没有配件，无法出库");
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
            int currentStock = part.getCurrentStock() != null ? part.getCurrentStock() : 0;

            if (currentStock < actualQuantity) {
                throw new RuntimeException("库存不足，配件[" + part.getName() + "]当前库存: " + currentStock + ", 需要: " + actualQuantity);
            }
        }

        String machineCode = null;
        if (request.getMachineId() != null) {
            PackagingMachine machine = packagingMachineMapper.selectById(request.getMachineId());
            if (machine == null) {
                throw new RuntimeException("机台不存在");
            }
            if (machine.getStatus() == null || machine.getStatus() != 1) {
                throw new RuntimeException("机台已停用，无法领用");
            }
            machineCode = machine.getMachineCode();
        }

        for (BucklePackageItem item : packageItems) {
            Part part = partMapper.selectById(item.getPartId());
            int actualQuantity = item.getQuantity() * packageQuantity;

            int affected = partMapper.deductStock(item.getPartId(), actualQuantity, now);
            if (affected == 0) {
                Part latest = partMapper.selectById(item.getPartId());
                int latestStock = latest != null && latest.getCurrentStock() != null ? latest.getCurrentStock() : 0;
                throw new RuntimeException("库存不足，配件[" + part.getName() + "]当前可用库存: " + latestStock);
            }

            OutboundRecord outboundRecord = new OutboundRecord();
            outboundRecord.setPartId(item.getPartId());
            outboundRecord.setQuantity(actualQuantity);
            outboundRecord.setProductionLine(request.getProductionLine());
            outboundRecord.setMachineId(request.getMachineId());
            outboundRecord.setMachineCode(machineCode);
            outboundRecord.setOperator(request.getOperator().trim());
            outboundRecord.setCreatedAt(now);
            outboundRecord.setPartName(part.getName());
            outboundRecord.setPartModel(part.getModel());
            outboundRecordMapper.insert(outboundRecord);

            redisCacheService.evictPartRelatedCache(part);
        }

        PackageOutboundRecord record = new PackageOutboundRecord();
        record.setPackageId(pkg.getId());
        record.setPackageName(pkg.getName());
        record.setPackageCode(pkg.getCode());
        record.setPackageQuantity(packageQuantity);
        record.setProductionLine(request.getProductionLine());
        record.setMachineId(request.getMachineId());
        record.setMachineCode(machineCode);
        record.setOperator(request.getOperator().trim());
        record.setRemark(request.getRemark() != null ? request.getRemark().trim() : null);
        record.setCreatedAt(now);
        packageOutboundRecordMapper.insert(record);

        if (record.getId() == null) {
            throw new RuntimeException("记录成套包出库流水失败");
        }

        for (BucklePackageItem item : packageItems) {
            Part part = partMapper.selectById(item.getPartId());
            int actualQuantity = item.getQuantity() * packageQuantity;

            PackageOutboundDetail detail = new PackageOutboundDetail();
            detail.setRecordId(record.getId());
            detail.setPartId(item.getPartId());
            detail.setPartName(part.getName());
            detail.setPartModel(part.getModel());
            detail.setQuantity(actualQuantity);
            packageOutboundDetailMapper.insert(detail);
        }

        redisCacheService.evictAllInventoryRelatedCache();
        return getOutboundById(record.getId());
    }

    private void validateRequest(PackageOutboundRequest request) {
        if (request == null) {
            throw new RuntimeException("出库请求不能为空");
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
        if (!StringUtils.hasText(request.getProductionLine())) {
            throw new RuntimeException("领用产线不能为空");
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
    }
}
