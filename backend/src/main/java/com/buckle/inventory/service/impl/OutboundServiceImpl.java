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
            Part part = partMapper.selectById(record.getPartId());
            if (part != null) {
                record.setPartName(part.getName());
                record.setPartModel(part.getModel());
            }
        }
        return pageResult;
    }

    @Override
    @Transactional
    public OutboundRecord addOutbound(OutboundRequest request) {
        Part part = partMapper.selectById(request.getPartId());
        if (part == null) {
            throw new RuntimeException("配件不存在");
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
}
