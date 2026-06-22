package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.ShelfMigrationRequest;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ShelfMigrationRecord;
import com.buckle.inventory.exception.ValidationException;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ShelfMigrationRecordMapper;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ShelfMigrationService;
import com.buckle.inventory.service.ShelfOccupancyService;
import com.buckle.inventory.util.ShelfPositionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShelfMigrationServiceImpl implements ShelfMigrationService {

    @Autowired
    private ShelfMigrationRecordMapper migrationRecordMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private ShelfOccupancyService shelfOccupancyService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<ShelfMigrationRecord> listMigrationRecords(int page, int size, String keyword) {
        LambdaQueryWrapper<ShelfMigrationRecord> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Part> matchedParts = partMapper.selectList(
                    new LambdaQueryWrapper<Part>()
                            .like(Part::getName, keyword)
                            .or()
                            .like(Part::getModel, keyword));
            if (!matchedParts.isEmpty()) {
                List<Long> partIds = matchedParts.stream().map(Part::getId).collect(Collectors.toList());
                queryWrapper.and(w -> w.in(ShelfMigrationRecord::getPartId, partIds)
                        .or().like(ShelfMigrationRecord::getPartName, keyword)
                        .or().like(ShelfMigrationRecord::getPartModel, keyword)
                        .or().like(ShelfMigrationRecord::getSourceShelf, keyword)
                        .or().like(ShelfMigrationRecord::getTargetShelf, keyword));
            } else {
                queryWrapper.and(w -> w.like(ShelfMigrationRecord::getPartName, keyword)
                        .or().like(ShelfMigrationRecord::getPartModel, keyword)
                        .or().like(ShelfMigrationRecord::getSourceShelf, keyword)
                        .or().like(ShelfMigrationRecord::getTargetShelf, keyword));
            }
        }

        queryWrapper.orderByDesc(ShelfMigrationRecord::getCreatedAt);
        Page<ShelfMigrationRecord> pageParam = new Page<>(page, size);
        Page<ShelfMigrationRecord> result = migrationRecordMapper.selectPage(pageParam, queryWrapper);
        PageResult<ShelfMigrationRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (ShelfMigrationRecord record : pageResult.getList()) {
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
    public ShelfMigrationRecord addShelfMigration(ShelfMigrationRequest request) {
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Part sourcePart = partMapper.selectById(request.getPartId());
        if (sourcePart == null) {
            throw new RuntimeException("配件不存在");
        }
        if (sourcePart.getDeleted() != null && sourcePart.getDeleted() == 1) {
            throw new RuntimeException("配件已删除，无法迁移");
        }

        int currentStock = sourcePart.getCurrentStock() != null ? sourcePart.getCurrentStock() : 0;
        int migrateQuantity = request.getQuantity();
        String sourceShelf = sourcePart.getShelfPosition();
        String targetShelf = request.getTargetShelf().trim();

        if (sourceShelf.equals(targetShelf)) {
            throw new RuntimeException("原货架与目标货架相同，无需迁移");
        }

        if (migrateQuantity > currentStock) {
            throw new RuntimeException("迁移数量不能超过当前库存，当前库存: " + currentStock);
        }

        boolean isNewTypeForShelf = true;
        LambdaQueryWrapper<Part> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Part::getShelfPosition, targetShelf)
                .eq(Part::getName, sourcePart.getName())
                .eq(Part::getModel, sourcePart.getModel())
                .eq(Part::getDeleted, 0);
        List<Part> existingPartsOnTarget = partMapper.selectList(queryWrapper);
        if (!existingPartsOnTarget.isEmpty()) {
            isNewTypeForShelf = false;
        }

        shelfOccupancyService.checkCapacity(targetShelf, migrateQuantity, isNewTypeForShelf);

        if (migrateQuantity == currentStock) {
            sourcePart.setShelfPosition(targetShelf);
            sourcePart.setUpdatedAt(now);
            int affected = partMapper.updateById(sourcePart);
            if (affected == 0) {
                throw new RuntimeException("更新配件货架位置失败");
            }

            redisCacheService.evictPartRelatedCache(sourcePart.getId(), null, null,
                    sourceShelf, sourcePart.getCategoryId());
            redisCacheService.evictPartRelatedCache(sourcePart.getId(), null, null,
                    targetShelf, sourcePart.getCategoryId());
        } else {
            int affected = partMapper.deductStock(sourcePart.getId(), migrateQuantity, now);
            if (affected == 0) {
                throw new RuntimeException("扣减原货架库存失败");
            }

            if (!existingPartsOnTarget.isEmpty()) {
                Part existingTargetPart = existingPartsOnTarget.get(0);
                int addAffected = partMapper.addStock(existingTargetPart.getId(), migrateQuantity, now);
                if (addAffected == 0) {
                    throw new RuntimeException("累加目标货架库存失败");
                }

                redisCacheService.evictPartRelatedCache(sourcePart.getId(), null, null,
                        sourceShelf, sourcePart.getCategoryId());
                redisCacheService.evictPartRelatedCache(existingTargetPart.getId(), null, null,
                        targetShelf, existingTargetPart.getCategoryId());
            } else {
                Part targetPart = new Part();
                targetPart.setCategoryId(sourcePart.getCategoryId());
                targetPart.setName(sourcePart.getName());
                targetPart.setModel(sourcePart.getModel());
                targetPart.setTotalQuantity(migrateQuantity);
                targetPart.setCurrentStock(migrateQuantity);
                targetPart.setShelfPosition(targetShelf);
                targetPart.setDeleted(0);
                targetPart.setCreatedAt(now);
                targetPart.setUpdatedAt(now);
                partMapper.insert(targetPart);

                if (targetPart.getId() == null) {
                    throw new RuntimeException("创建目标货架配件记录失败");
                }

                redisCacheService.evictPartRelatedCache(sourcePart.getId(), null, null,
                        sourceShelf, sourcePart.getCategoryId());
                redisCacheService.evictPartRelatedCache(targetPart.getId(), null, null,
                        targetShelf, targetPart.getCategoryId());
            }
        }

        ShelfMigrationRecord record = new ShelfMigrationRecord();
        record.setPartId(sourcePart.getId());
        record.setPartName(sourcePart.getName());
        record.setPartModel(sourcePart.getModel());
        record.setSourceShelf(sourceShelf);
        record.setTargetShelf(targetShelf);
        record.setQuantity(migrateQuantity);
        record.setOperator(request.getOperator().trim());
        record.setCreatedAt(now);
        migrationRecordMapper.insert(record);

        if (record.getId() == null) {
            throw new RuntimeException("记录货架迁移流水失败");
        }

        return record;
    }

    private void validateRequest(ShelfMigrationRequest request) {
        if (request == null) {
            throw new RuntimeException("迁移请求不能为空");
        }
        if (request.getPartId() == null) {
            throw new RuntimeException("请选择要迁移的配件");
        }
        if (request.getQuantity() == null) {
            throw new RuntimeException("迁移数量不能为空");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("迁移数量必须大于0，当前值: " + request.getQuantity());
        }
        if (!StringUtils.hasText(request.getTargetShelf())) {
            throw new RuntimeException("目标货架位置不能为空");
        }
        if (!ShelfPositionValidator.isValid(request.getTargetShelf())) {
            throw new ValidationException("targetShelf", "货架位置" + ShelfPositionValidator.FORMAT_HINT);
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
    }
}
