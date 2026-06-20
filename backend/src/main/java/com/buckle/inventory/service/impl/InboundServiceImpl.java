package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.InboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.InboundService;
import com.buckle.inventory.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<InboundRecord> listInbound(int page, int size, String keyword) {
        LambdaQueryWrapper<InboundRecord> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Part> matchedParts = partMapper.selectList(
                    new LambdaQueryWrapper<Part>()
                            .like(Part::getName, keyword)
                            .or()
                            .like(Part::getModel, keyword));
            if (!matchedParts.isEmpty()) {
                List<Long> partIds = matchedParts.stream().map(Part::getId).collect(Collectors.toList());
                queryWrapper.and(w -> w.in(InboundRecord::getPartId, partIds)
                        .or().like(InboundRecord::getPartName, keyword)
                        .or().like(InboundRecord::getPartModel, keyword));
            } else {
                queryWrapper.and(w -> w.like(InboundRecord::getPartName, keyword)
                        .or().like(InboundRecord::getPartModel, keyword));
            }
        }

        queryWrapper.orderByDesc(InboundRecord::getCreatedAt);
        Page<InboundRecord> pageParam = new Page<>(page, size);
        Page<InboundRecord> result = inboundRecordMapper.selectPage(pageParam, queryWrapper);
        PageResult<InboundRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (InboundRecord record : pageResult.getList()) {
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
    public InboundRecord addInbound(InboundRequest request) {
        validateRequest(request);

        Part part;
        int inboundQuantity = request.getQuantity();

        if (request.getPartId() != null) {
            part = partMapper.selectById(request.getPartId());
            if (part == null) {
                throw new RuntimeException("配件不存在");
            }
            if (part.getDeleted() != null && part.getDeleted() == 1) {
                throw new RuntimeException("配件已删除，无法入库");
            }

            int oldTotal = part.getTotalQuantity() != null ? part.getTotalQuantity() : 0;
            int oldStock = part.getCurrentStock() != null ? part.getCurrentStock() : 0;

            int newTotal = oldTotal + inboundQuantity;
            int newStock = oldStock + inboundQuantity;

            part.setTotalQuantity(newTotal);
            part.setCurrentStock(newStock);
            if (StringUtils.hasText(request.getShelfPosition())) {
                part.setShelfPosition(request.getShelfPosition());
            }
            part.setUpdatedAt(LocalDateTime.now());
            int updateRows = partMapper.updateById(part);
            if (updateRows == 0) {
                throw new RuntimeException("更新配件库存失败，请重试");
            }

            Part updatedPart = partMapper.selectById(part.getId());
            if (updatedPart.getTotalQuantity() != newTotal || updatedPart.getCurrentStock() != newStock) {
                throw new RuntimeException("库存更新后一致性校验失败");
            }

            verifyPartConsistency(part.getId(), newTotal, newStock);
        } else if (StringUtils.hasText(request.getPartName()) && StringUtils.hasText(request.getPartModel())) {
            part = new Part();
            part.setName(request.getPartName().trim());
            part.setModel(request.getPartModel().trim());
            part.setTotalQuantity(inboundQuantity);
            part.setCurrentStock(inboundQuantity);
            part.setShelfPosition(request.getShelfPosition() != null ? request.getShelfPosition() : "");
            part.setCreatedAt(LocalDateTime.now());
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.insert(part);

            if (part.getId() == null) {
                throw new RuntimeException("创建配件失败");
            }
        } else {
            throw new RuntimeException("请提供配件ID或配件名称和型号");
        }

        InboundRecord record = new InboundRecord();
        record.setPartId(part.getId());
        record.setQuantity(inboundQuantity);
        record.setShelfPosition(request.getShelfPosition());
        record.setOperator(request.getOperator() != null ? request.getOperator().trim() : "system");
        record.setCreatedAt(LocalDateTime.now());
        record.setPartName(part.getName());
        record.setPartModel(part.getModel());
        inboundRecordMapper.insert(record);

        if (record.getId() == null) {
            throw new RuntimeException("记录入库流水失败");
        }

        redisCacheService.refreshPartsCache();
        return record;
    }

    private void validateRequest(InboundRequest request) {
        if (request == null) {
            throw new RuntimeException("入库请求不能为空");
        }
        if (request.getQuantity() == null) {
            throw new RuntimeException("入库数量不能为空");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("入库数量必须大于0，当前值: " + request.getQuantity());
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
    }

    private void verifyPartConsistency(Long partId, int expectedTotal, int expectedCurrentStock) {
        Integer totalInbound = inboundRecordMapper.selectList(
                        new LambdaQueryWrapper<InboundRecord>().eq(InboundRecord::getPartId, partId))
                .stream()
                .mapToInt(InboundRecord::getQuantity)
                .sum();

        Integer totalOutbound = outboundRecordMapper.selectList(
                        new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getPartId, partId))
                .stream()
                .mapToInt(OutboundRecord::getQuantity)
                .sum();

        Integer totalScrap = scrapRecordMapper.selectList(
                        new LambdaQueryWrapper<ScrapRecord>().eq(ScrapRecord::getPartId, partId))
                .stream()
                .mapToInt(ScrapRecord::getQuantity)
                .sum();

        int calculatedTotal = totalInbound;
        int calculatedStock = totalInbound - totalOutbound - totalScrap;

        if (calculatedTotal != expectedTotal) {
            throw new RuntimeException(
                    String.format("总量一致性校验失败: 流水汇总=%d, 预期=%d", calculatedTotal, expectedTotal));
        }
        if (calculatedStock != expectedCurrentStock) {
            throw new RuntimeException(
                    String.format("库存一致性校验失败: 流水计算=%d, 预期=%d", calculatedStock, expectedCurrentStock));
        }
    }
}
