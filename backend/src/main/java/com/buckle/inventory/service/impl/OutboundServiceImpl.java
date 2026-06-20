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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class OutboundServiceImpl implements OutboundService {

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private PartMapper partMapper;

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
    @Transactional
    public OutboundRecord addOutbound(OutboundRequest request) {
        validateRequest(request);

        Part part = partMapper.selectById(request.getPartId());
        if (part == null) {
            throw new RuntimeException("配件不存在");
        }
        if (part.getDeleted() != null && part.getDeleted() == 1) {
            throw new RuntimeException("配件已删除，无法出库");
        }
        if (part.getCurrentStock() < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前库存: " + part.getCurrentStock());
        }

        part.setCurrentStock(part.getCurrentStock() - request.getQuantity());
        part.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(part);

        OutboundRecord record = new OutboundRecord();
        record.setPartId(request.getPartId());
        record.setQuantity(request.getQuantity());
        record.setProductionLine(request.getProductionLine());
        record.setOperator(request.getOperator());
        record.setCreatedAt(LocalDateTime.now());
        record.setPartName(part.getName());
        record.setPartModel(part.getModel());
        outboundRecordMapper.insert(record);

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
}
