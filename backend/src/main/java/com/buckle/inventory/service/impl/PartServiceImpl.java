package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PartQueryDTO;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.PartService;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ShelfOccupancyService;
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

    @Override
    public PageResult<Part> listParts(PartQueryDTO query) {
        Page<Part> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1);
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Part::getName, query.getName());
        }
        if (StringUtils.hasText(query.getModel())) {
            wrapper.like(Part::getModel, query.getModel());
        }
        if (StringUtils.hasText(query.getShelfPosition())) {
            wrapper.eq(Part::getShelfPosition, query.getShelfPosition());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Part::getCategoryId, query.getCategoryId());
        }
        wrapper.orderByDesc(Part::getCreatedAt);
        Page<Part> result = partMapper.selectPage(page, wrapper);
        populateCategoryNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), query.getPage(), query.getSize());
    }

    private void populateCategoryNames(List<Part> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<Long> categoryIds = parts.stream()
                .map(Part::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return;
        }
        List<AccessoryCategory> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(AccessoryCategory::getId, AccessoryCategory::getName));
        for (Part part : parts) {
            if (part.getCategoryId() != null) {
                part.setCategoryName(categoryNameMap.get(part.getCategoryId()));
            }
        }
    }

    @Override
    public Part getPartById(Long id) {
        Part part = partMapper.selectById(id);
        if (part != null && part.getCategoryId() != null) {
            AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
            if (category != null) {
                part.setCategoryName(category.getName());
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
        redisCacheService.refreshPartsCache();
        return part;
    }

    @Override
    public Part updatePart(Part part) {
        part.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(part);
        redisCacheService.refreshPartsCache();
        return part;
    }

    @Override
    public void deletePart(Long id) {
        Part part = partMapper.selectById(id);
        if (part != null) {
            part.setDeleted(1);
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.updateById(part);
        }
        redisCacheService.refreshPartsCache();
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
        }
        redisCacheService.refreshPartsCache();
        return parts;
    }

    @Override
    public List<Part> getAllParts() {
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1);
        List<Part> parts = partMapper.selectList(wrapper);
        populateCategoryNames(parts);
        return parts;
    }
}
