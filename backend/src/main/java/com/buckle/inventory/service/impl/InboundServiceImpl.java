package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.InboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.InboundService;
import com.buckle.inventory.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InboundServiceImpl implements InboundService {

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<InboundRecord> listInbound(int page, int size, String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundRecord> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Part> matchedParts = partMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Part>()
                            .like(Part::getName, keyword)
                            .or()
                            .like(Part::getModel, keyword));
            if (matchedParts.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            List<Long> partIds = matchedParts.stream().map(Part::getId).collect(Collectors.toList());
            queryWrapper.in(InboundRecord::getPartId, partIds);
        }

        queryWrapper.orderByDesc(InboundRecord::getCreatedAt);
        Page<InboundRecord> pageParam = new Page<>(page, size);
        Page<InboundRecord> result = inboundRecordMapper.selectPage(pageParam, queryWrapper);
        PageResult<InboundRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (InboundRecord record : pageResult.getList()) {
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
    public InboundRecord addInbound(InboundRequest request) {
        Part part;
        if (request.getPartId() != null) {
            part = partMapper.selectById(request.getPartId());
            if (part == null) {
                throw new RuntimeException("配件不存在");
            }
            part.setCurrentStock(part.getCurrentStock() + request.getQuantity());
            part.setTotalQuantity(part.getTotalQuantity() + request.getQuantity());
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.updateById(part);
        } else if (request.getPartName() != null && request.getPartModel() != null) {
            part = new Part();
            part.setName(request.getPartName());
            part.setModel(request.getPartModel());
            part.setTotalQuantity(request.getQuantity());
            part.setCurrentStock(request.getQuantity());
            part.setShelfPosition(request.getShelfPosition() != null ? request.getShelfPosition() : "");
            part.setCreatedAt(LocalDateTime.now());
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.insert(part);
        } else {
            throw new RuntimeException("请提供配件ID或配件名称和型号");
        }

        InboundRecord record = new InboundRecord();
        record.setPartId(part.getId());
        record.setQuantity(request.getQuantity());
        record.setShelfPosition(request.getShelfPosition());
        record.setOperator(request.getOperator());
        record.setCreatedAt(LocalDateTime.now());
        record.setPartName(part.getName());
        record.setPartModel(part.getModel());
        inboundRecordMapper.insert(record);

        redisCacheService.refreshPartsCache();
        return record;
    }
}
