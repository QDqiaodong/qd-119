package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PartQueryDTO;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.PartService;
import com.buckle.inventory.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartServiceImpl implements PartService {

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private RedisCacheService redisCacheService;

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
        wrapper.orderByDesc(Part::getCreatedAt);
        Page<Part> result = partMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public Part getPartById(Long id) {
        return partMapper.selectById(id);
    }

    @Override
    public Part addPart(Part part) {
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
        part.setCreatedAt(LocalDateTime.now());
        part.setUpdatedAt(LocalDateTime.now());
        partMapper.insert(part);
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
            part.setCreatedAt(LocalDateTime.now());
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.insert(part);
        }
        redisCacheService.refreshPartsCache();
        return parts;
    }

    @Override
    public List<Part> getAllParts() {
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Part::getDeleted, 1);
        return partMapper.selectList(wrapper);
    }
}
