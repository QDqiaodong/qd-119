package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PartDeletionCheckDTO;
import com.buckle.inventory.dto.PartQueryDTO;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.InventoryCheckItem;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.InventoryCheckItemMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.PartService;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ShelfOccupancyService;
import com.buckle.inventory.exception.ValidationException;
import com.buckle.inventory.util.ShelfPositionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PartServiceImpl implements PartService {

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Autowired
    private ShelfOccupancyService shelfOccupancyService;

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private InventoryCheckItemMapper inventoryCheckItemMapper;

    @Override
    public PageResult<Part> listParts(PartQueryDTO query) {
        boolean hasNameFilter = StringUtils.hasText(query.getName());
        boolean hasModelFilter = StringUtils.hasText(query.getModel());
        boolean hasShelfFilter = StringUtils.hasText(query.getShelfPosition());
        boolean hasCategoryFilter = query.getCategoryId() != null;
        boolean isPureShelfOrCategory = !hasNameFilter && !hasModelFilter
                && (hasShelfFilter || hasCategoryFilter);

        List<Part> allFilteredParts = null;
        if (isPureShelfOrCategory) {
            allFilteredParts = queryFromRedisShards(query.getShelfPosition(), query.getCategoryId());
        }

        Page<Part> result;
        if (allFilteredParts != null) {
            int pageNum = Math.max(query.getPage(), 1);
            int pageSize = Math.max(query.getSize(), 1);
            long total = allFilteredParts.size();
            int fromIndex = Math.min((pageNum - 1) * pageSize, (int) total);
            int toIndex = Math.min(fromIndex + pageSize, (int) total);
            List<Part> pageRecords = allFilteredParts.subList(fromIndex, toIndex);
            pageRecords = new java.util.ArrayList<>(pageRecords);
            populateCategoryNames(pageRecords);
            return new PageResult<>(pageRecords, total, pageNum, pageSize);
        }

        Page<Part> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1);
        if (hasNameFilter) {
            wrapper.like(Part::getName, query.getName());
        }
        if (hasModelFilter) {
            wrapper.like(Part::getModel, query.getModel());
        }
        if (hasShelfFilter) {
            wrapper.eq(Part::getShelfPosition, query.getShelfPosition());
        }
        if (hasCategoryFilter) {
            wrapper.eq(Part::getCategoryId, query.getCategoryId());
        }
        wrapper.orderByDesc(Part::getCreatedAt);
        result = partMapper.selectPage(page, wrapper);
        populateCategoryNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), query.getPage(), query.getSize());
    }

    private List<Part> queryFromRedisShards(String shelfPosition, Long categoryId) {
        boolean hasShelf = StringUtils.hasText(shelfPosition);
        boolean hasCategory = categoryId != null;

        List<Part> shelfParts = null;
        List<Part> categoryParts = null;

        if (hasShelf) {
            try {
                shelfParts = redisCacheService.getPartsByShelfPosition(shelfPosition);
                logCacheProbe("READ_SHARD", "parts:shelf:" + shelfPosition, shelfParts == null ? 0 : shelfParts.size());
            } catch (Exception e) {
                logCacheProbe("READ_SHARD_FAIL", "parts:shelf:" + shelfPosition, -1);
                return null;
            }
        }
        if (hasCategory) {
            try {
                categoryParts = redisCacheService.getPartsByCategoryId(categoryId);
                logCacheProbe("READ_SHARD", "parts:category:" + categoryId, categoryParts == null ? 0 : categoryParts.size());
            } catch (Exception e) {
                logCacheProbe("READ_SHARD_FAIL", "parts:category:" + categoryId, -1);
                return null;
            }
        }

        List<Part> result;
        if (hasShelf && hasCategory) {
            if (shelfParts == null || categoryParts == null) return null;
            final java.util.Set<Long> categoryPartIds = categoryParts.stream()
                    .map(Part::getId)
                    .collect(Collectors.toSet());
            result = shelfParts.stream()
                    .filter(p -> categoryPartIds.contains(p.getId()))
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            logCacheProbe("SHARD_INTERSECT", "shelf:" + shelfPosition + "|category:" + categoryId, result.size());
        } else if (hasShelf) {
            result = shelfParts == null ? null : shelfParts.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        } else if (hasCategory) {
            result = categoryParts == null ? null : categoryParts.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        } else {
            return null;
        }
        return result;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PartServiceImpl.class);

    private void logCacheProbe(String action, String key, int sizeOrStatus) {
        log.info("[CACHE_PROBE] action={} key={} value={}", action, key, sizeOrStatus);
    }

    private void populateCategoryNames(List<Part> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<Long> categoryIds;
        try {
            categoryIds = parts.stream()
                    .map(Part::getCategoryId)
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[populateCategoryNames] extract categoryIds failed: {}", e.getMessage());
            return;
        }
        if (categoryIds.isEmpty()) {
            return;
        }
        List<AccessoryCategory> categories;
        try {
            categories = categoryMapper.selectBatchIds(categoryIds);
        } catch (Exception e) {
            log.warn("[populateCategoryNames] query categories failed: {}", e.getMessage());
            return;
        }
        if (categories == null || categories.isEmpty()) {
            return;
        }
        Map<Long, String> categoryNameMap;
        try {
            categoryNameMap = categories.stream()
                    .filter(c -> c != null && c.getId() != null)
                    .collect(Collectors.toMap(AccessoryCategory::getId, AccessoryCategory::getName, (a, b) -> a));
        } catch (Exception e) {
            log.warn("[populateCategoryNames] build categoryNameMap failed: {}", e.getMessage());
            return;
        }
        for (Part part : parts) {
            if (part == null) continue;
            try {
                if (part.getCategoryId() != null && part.getCategoryId() > 0) {
                    String name = categoryNameMap.get(part.getCategoryId());
                    if (name != null) {
                        part.setCategoryName(name);
                    }
                }
            } catch (Exception e) {
                log.warn("[populateCategoryNames] set categoryName failed for part {}: {}",
                        part.getId(), e.getMessage());
            }
        }
    }

    @Override
    public Part getPartById(Long id) {
        Part part;
        try {
            part = partMapper.selectById(id);
        } catch (Exception e) {
            log.warn("[getPartById] selectById failed: {}", e.getMessage());
            return null;
        }
        if (part != null && part.getCategoryId() != null && part.getCategoryId() > 0) {
            try {
                AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
                if (category != null) {
                    part.setCategoryName(category.getName());
                }
            } catch (Exception e) {
                log.warn("[getPartById] query category failed: {}", e.getMessage());
            }
        }
        return part;
    }

    @Override
    public Part addPart(Part part) {
        if (part.getCategoryId() == null) {
            throw new RuntimeException("请选择配件类别");
        }
        AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
        if (category == null) {
            throw new RuntimeException("配件类别不存在");
        }
        if (!StringUtils.hasText(part.getName())) {
            throw new RuntimeException("配件名称不能为空");
        }
        if (!StringUtils.hasText(part.getModel())) {
            throw new RuntimeException("配件型号不能为空");
        }
        if (!ShelfPositionValidator.isValid(part.getShelfPosition())) {
            throw new ValidationException("shelfPosition", "货架位置" + ShelfPositionValidator.FORMAT_HINT);
        }
        if (part.getTotalQuantity() == null) {
            part.setTotalQuantity(0);
        }
        if (part.getCurrentStock() == null) {
            part.setCurrentStock(part.getTotalQuantity());
        }
        if (part.getShelfPosition() == null) {
            part.setShelfPosition("");
        }
        if (part.getDeleted() == null) {
            part.setDeleted(0);
        }

        int additionalQuantity = part.getCurrentStock() != null ? part.getCurrentStock() : 0;
        shelfOccupancyService.checkCapacity(part.getShelfPosition(), additionalQuantity, true);

        part.setCreatedAt(LocalDateTime.now());
        part.setUpdatedAt(LocalDateTime.now());
        partMapper.insert(part);
        part.setCategoryName(category.getName());
        redisCacheService.evictPartRelatedCache(null, null, null,
                part.getShelfPosition(), part.getCategoryId());
        return part;
    }

    @Override
    public Part updatePart(Part part) {
        if (!StringUtils.hasText(part.getName())) {
            throw new RuntimeException("配件名称不能为空");
        }
        if (!StringUtils.hasText(part.getModel())) {
            throw new RuntimeException("配件型号不能为空");
        }
        if (part.getCategoryId() == null) {
            throw new RuntimeException("请选择配件类别");
        }
        AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
        if (category == null) {
            throw new RuntimeException("配件类别不存在");
        }
        if (!ShelfPositionValidator.isValid(part.getShelfPosition())) {
            throw new ValidationException("shelfPosition", "货架位置" + ShelfPositionValidator.FORMAT_HINT);
        }
        if (part.getCurrentStock() != null && part.getCurrentStock() < 0) {
            throw new RuntimeException("当前库存不能为负数");
        }
        if (part.getTotalQuantity() != null && part.getTotalQuantity() < 0) {
            throw new RuntimeException("累计入库量不能为负数");
        }

        Part oldPart = partMapper.selectById(part.getId());
        if (oldPart == null) {
            throw new RuntimeException("配件不存在");
        }
        String oldShelfPosition = oldPart.getShelfPosition();
        Long oldCategoryId = oldPart.getCategoryId();
        int oldCurrentStock = oldPart.getCurrentStock() != null ? oldPart.getCurrentStock() : 0;
        int newCurrentStock = part.getCurrentStock() != null ? part.getCurrentStock() : 0;

        boolean shelfChanged = oldShelfPosition != null && !oldShelfPosition.equals(part.getShelfPosition());
        if (shelfChanged) {
            int additionalQuantity = newCurrentStock;
            boolean isNewTypeForShelf = true;
            LambdaQueryWrapper<Part> sameTypeWrapper = new LambdaQueryWrapper<>();
            sameTypeWrapper.eq(Part::getShelfPosition, part.getShelfPosition())
                    .eq(Part::getName, part.getName())
                    .eq(Part::getModel, part.getModel())
                    .eq(Part::getDeleted, 0)
                    .ne(Part::getId, part.getId());
            List<Part> sameTypeOnTarget = partMapper.selectList(sameTypeWrapper);
            if (!sameTypeOnTarget.isEmpty()) {
                isNewTypeForShelf = false;
                additionalQuantity = Math.max(0, newCurrentStock);
            }
            shelfOccupancyService.checkCapacity(part.getShelfPosition(), additionalQuantity, isNewTypeForShelf);
        } else {
            int stockDiff = newCurrentStock - oldCurrentStock;
            if (stockDiff > 0) {
                shelfOccupancyService.checkCapacity(part.getShelfPosition(), stockDiff, false);
            }
        }

        part.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(part);
        redisCacheService.evictPartRelatedCache(part.getId(), oldShelfPosition, oldCategoryId,
                part.getShelfPosition(), part.getCategoryId());
        return part;
    }

    @Override
    public PartDeletionCheckDTO deletePart(Long id) {
        PartDeletionCheckDTO checkDTO = checkDeletionAllowed(id);
        if (!checkDTO.isCanDelete()) {
            return checkDTO;
        }
        Part part = partMapper.selectById(id);
        if (part != null) {
            String oldShelfPosition = part.getShelfPosition();
            Long oldCategoryId = part.getCategoryId();
            part.setDeleted(1);
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.updateById(part);
            redisCacheService.evictPartRelatedCache(id, oldShelfPosition, oldCategoryId, null, null);
        }
        checkDTO.setCanDelete(true);
        return checkDTO;
    }

    @Override
    public PartDeletionCheckDTO checkDeletionAllowed(Long id) {
        PartDeletionCheckDTO dto = new PartDeletionCheckDTO();

        LambdaQueryWrapper<InboundRecord> inboundWrapper = new LambdaQueryWrapper<>();
        inboundWrapper.eq(InboundRecord::getPartId, id);
        dto.setInboundCount(Math.toIntExact(inboundRecordMapper.selectCount(inboundWrapper)));

        LambdaQueryWrapper<OutboundRecord> outboundWrapper = new LambdaQueryWrapper<>();
        outboundWrapper.eq(OutboundRecord::getPartId, id);
        dto.setOutboundCount(Math.toIntExact(outboundRecordMapper.selectCount(outboundWrapper)));

        LambdaQueryWrapper<ScrapRecord> scrapWrapper = new LambdaQueryWrapper<>();
        scrapWrapper.eq(ScrapRecord::getPartId, id);
        dto.setScrapCount(Math.toIntExact(scrapRecordMapper.selectCount(scrapWrapper)));

        LambdaQueryWrapper<InventoryCheckItem> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(InventoryCheckItem::getPartId, id);
        dto.setInventoryCheckCount(Math.toIntExact(inventoryCheckItemMapper.selectCount(checkWrapper)));

        dto.setCanDelete(dto.getTotalRelatedCount() == 0);
        return dto;
    }

    @Override
    public List<Part> batchAddParts(List<Part> parts) {
        for (Part part : parts) {
            if (part.getCategoryId() == null) {
                throw new RuntimeException("请选择配件类别: " + (part.getName() != null ? part.getName() : ""));
            }
            AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
            if (category == null) {
                throw new RuntimeException("配件类别不存在: " + part.getCategoryId());
            }
            if (!StringUtils.hasText(part.getName())) {
                throw new RuntimeException("配件名称不能为空");
            }
            if (!StringUtils.hasText(part.getModel())) {
                throw new RuntimeException("配件型号不能为空");
            }
            if (!ShelfPositionValidator.isValid(part.getShelfPosition())) {
                throw new ValidationException("shelfPosition", "货架位置" + ShelfPositionValidator.FORMAT_HINT + (part.getName() != null ? "（配件：" + part.getName() + "）" : ""));
            }
            if (part.getTotalQuantity() == null) {
                part.setTotalQuantity(0);
            }
            if (part.getCurrentStock() == null) {
                part.setCurrentStock(part.getTotalQuantity());
            }
            if (part.getShelfPosition() == null) {
                part.setShelfPosition("");
            }
            if (part.getDeleted() == null) {
                part.setDeleted(0);
            }

            int additionalQuantity = part.getCurrentStock() != null ? part.getCurrentStock() : 0;
            shelfOccupancyService.checkCapacity(part.getShelfPosition(), additionalQuantity, true);

            part.setCreatedAt(LocalDateTime.now());
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.insert(part);
            part.setCategoryName(category.getName());
            redisCacheService.evictPartRelatedCache(null, null, null,
                    part.getShelfPosition(), part.getCategoryId());
        }
        return parts;
    }

    @Override
    public List<Part> getAllParts() {
        List<Part> parts;
        try {
            LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(Part::getDeleted, 1);
            parts = partMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("[getAllParts] selectList failed: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
        populateCategoryNames(parts);
        return parts;
    }
}
