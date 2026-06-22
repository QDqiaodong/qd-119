package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.InboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.InboundService;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ShelfOccupancyService;
import com.buckle.inventory.exception.ValidationException;
import com.buckle.inventory.util.ShelfPositionValidator;
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

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Autowired
    private ShelfOccupancyService shelfOccupancyService;

    @Override
    public InboundRecord getById(Long id) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record != null && (record.getPartName() == null || record.getPartModel() == null)) {
            Part part = partMapper.selectById(record.getPartId());
            if (part != null) {
                if (record.getPartName() == null) {
                    record.setPartName(part.getName());
                }
                if (record.getPartModel() == null) {
                    record.setPartModel(part.getModel());
                }
            }
        }
        return record;
    }

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
        LocalDateTime now = LocalDateTime.now();

        if (request.getPartId() != null) {
            part = partMapper.selectById(request.getPartId());
            if (part == null) {
                throw new RuntimeException("配件不存在");
            }
            if (part.getDeleted() != null && part.getDeleted() == 1) {
                throw new RuntimeException("配件已删除，无法入库");
            }

            String targetShelf = StringUtils.hasText(request.getShelfPosition())
                    ? request.getShelfPosition()
                    : part.getShelfPosition();
            boolean shelfChanged = StringUtils.hasText(targetShelf)
                    && !targetShelf.equals(part.getShelfPosition());
            boolean isNewTypeForShelf = shelfChanged;
            shelfOccupancyService.checkCapacity(targetShelf, inboundQuantity, isNewTypeForShelf);

            int affected = partMapper.addStock(request.getPartId(), inboundQuantity, now);
            if (affected == 0) {
                throw new RuntimeException("更新配件库存失败，请重试");
            }

            if (shelfChanged) {
                part.setShelfPosition(targetShelf);
                part.setUpdatedAt(now);
                partMapper.updateById(part);
            }

            part = partMapper.selectById(request.getPartId());
        } else if (StringUtils.hasText(request.getPartName()) && StringUtils.hasText(request.getPartModel())) {
            if (request.getCategoryId() == null) {
                throw new RuntimeException("请选择配件类别");
            }
            AccessoryCategory category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw new RuntimeException("配件类别不存在");
            }

            String targetShelf = request.getShelfPosition() != null ? request.getShelfPosition() : "";
            shelfOccupancyService.checkCapacity(targetShelf, inboundQuantity, true);

            part = new Part();
            part.setCategoryId(request.getCategoryId());
            part.setName(request.getPartName().trim());
            part.setModel(request.getPartModel().trim());
            part.setTotalQuantity(inboundQuantity);
            part.setCurrentStock(inboundQuantity);
            part.setShelfPosition(targetShelf);
            part.setCreatedAt(now);
            part.setUpdatedAt(now);
            part.setDeleted(0);
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
        record.setCreatedAt(now);
        record.setPartName(part.getName());
        record.setPartModel(part.getModel());
        inboundRecordMapper.insert(record);

        if (record.getId() == null) {
            throw new RuntimeException("记录入库流水失败");
        }

        verifyPartConsistency(part.getId());

        redisCacheService.evictPartRelatedCache(part.getId(), null, null,
                part.getShelfPosition(), part.getCategoryId());
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
        if (!ShelfPositionValidator.isValid(request.getShelfPosition())) {
            throw new ValidationException("shelfPosition", "货架位置" + ShelfPositionValidator.FORMAT_HINT);
        }
    }

    private void verifyPartConsistency(Long partId) {
        Part part = partMapper.selectById(partId);
        if (part == null) {
            return;
        }

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

        Integer expectedTotal = part.getTotalQuantity() != null ? part.getTotalQuantity() : 0;
        Integer expectedCurrentStock = part.getCurrentStock() != null ? part.getCurrentStock() : 0;

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
