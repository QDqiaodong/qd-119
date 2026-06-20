package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.service.AccessoryCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccessoryCategoryServiceImpl implements AccessoryCategoryService {

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Override
    public List<AccessoryCategory> listAll() {
        LambdaQueryWrapper<AccessoryCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AccessoryCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public AccessoryCategory getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public AccessoryCategory add(AccessoryCategory category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public AccessoryCategory update(AccessoryCategory category) {
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}
