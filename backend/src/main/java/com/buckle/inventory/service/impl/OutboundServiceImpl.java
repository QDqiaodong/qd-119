package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.OutboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.OutboundService;
import com.buckle.inventory.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class OutboundServiceImpl implements OutboundService {

    private static final Logger log = LoggerFactory.getLogger(OutboundServiceImpl.class);

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<OutboundRecord> listOutbound(int page, int size, String productionLine) {
        Page<OutboundRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OutboundRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productionLine)) {
            wrapper.eq(OutboundRecord::getProductionLine, productionLine);
        }
        wrapper.orderByDesc(OutboundRecord::getCreatedAt);
        Page<OutboundRecord> result = outboundRecordMapper.selectPage(pageParam, wrapper);
        PageResult<OutboundRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (OutboundRecord record : pageResult.getList()) {
            if (record.getPartName() == null || record.getPartModel() == null) {
                Part part = partMapper.selectById(record.getPartId());
                if (part != null) {
                    if (record.getPartName() == null) {
                        record.setPartName(part.getName());
                    }
                    if (record.getPartModel() == null) {
                        record.setPartModel(part.getModel());
                    }
                } else {
                    if (record.getPartName() == null) {
                        record.setPartName("未知配件");
                    }
                    if (record.getPartModel() == null) {
                        record.setPartModel("-");
                    }
                }
            }
        }
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundRecord addOutbound(OutboundRequest request) {
        validateRequest(request);

        String idempotentKey = generateIdempotentKey(request);
        Long existingRecordId = redisCacheService.getOutboundIdempotentRecord(idempotentKey);
        if (existingRecordId != null) {
            OutboundRecord existingRecord = outboundRecordMapper.selectById(existingRecordId);
            if (existingRecord != null) {
                log.info("[addOutbound] idempotent hit, returning existing record: key={}, recordId={}",
                        idempotentKey, existingRecordId);
                return existingRecord;
            }
        }

        Part part = partMapper.selectById(request.getPartId());
        if (part == null) {
            throw new RuntimeException("配件不存在");
        }
        if (part.getDeleted() != null && part.getDeleted() == 1) {
            throw new RuntimeException("配件已删除，无法出库");
        }

        int currentStockBefore = part.getCurrentStock() != null ? part.getCurrentStock() : 0;
        if (currentStockBefore < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前库存: " + currentStockBefore);
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = partMapper.deductStock(request.getPartId(), request.getQuantity(), now);
        if (affected == 0) {
            Part latest = partMapper.selectById(request.getPartId());
            int latestStock = latest != null && latest.getCurrentStock() != null ? latest.getCurrentStock() : 0;
            throw new RuntimeException("库存不足，当前可用库存: " + latestStock);
        }

        Part updatedPart = partMapper.selectById(request.getPartId());

        OutboundRecord record = new OutboundRecord();
        record.setPartId(request.getPartId());
        record.setQuantity(request.getQuantity());
        record.setProductionLine(request.getProductionLine());
        record.setOperator(request.getOperator());
        record.setCreatedAt(now);
        record.setPartName(updatedPart.getName());
        record.setPartModel(updatedPart.getModel());
        outboundRecordMapper.insert(record);

        redisCacheService.setOutboundIdempotentRecord(idempotentKey, record.getId());

        redisCacheService.evictPartRelatedCache(updatedPart.getId(), null, null,
                updatedPart.getShelfPosition(), updatedPart.getCategoryId());
        return record;
    }

    private void validateRequest(OutboundRequest request) {
        if (request == null) {
            throw new RuntimeException("出库请求不能为空");
        }
        if (request.getQuantity() == null) {
            throw new RuntimeException("出库数量不能为空");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("出库数量必须大于0，当前值: " + request.getQuantity());
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
    }

    private String generateIdempotentKey(OutboundRequest request) {
        String productionLine = StringUtils.hasText(request.getProductionLine()) ? request.getProductionLine() : "";
        String partId = request.getPartId() != null ? request.getPartId().toString() : "";
        String quantity = request.getQuantity() != null ? request.getQuantity().toString() : "";
        return productionLine + ":" + partId + ":" + quantity;
    }
}
