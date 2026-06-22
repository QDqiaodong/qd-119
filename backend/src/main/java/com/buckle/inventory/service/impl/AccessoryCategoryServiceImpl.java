package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.AccessoryCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccessoryCategoryServiceImpl implements AccessoryCategoryService {

    private static final String BUCKLE_CATEGORY_CODE = "BUCKLE";
    private static final String BRACKET_CATEGORY_CODE = "BRACKET";

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Autowired
    private PartMapper partMapper;

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
        AccessoryCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("类别不存在");
        }
        if (BUCKLE_CATEGORY_CODE.equals(category.getCode()) || BRACKET_CATEGORY_CODE.equals(category.getCode())) {
            throw new RuntimeException("系统内置类别（卡扣/支架）不能删除");
        }
        LambdaQueryWrapper<Part> partWrapper = new LambdaQueryWrapper<>();
        partWrapper.eq(Part::getCategoryId, id).ne(Part::getDeleted, 1);
        Long partCount = partMapper.selectCount(partWrapper);
        if (partCount != null && partCount > 0) {
            throw new RuntimeException("该类别下存在 " + partCount + " 个配件，无法删除。请先移除或重新归类这些配件。");
        }
        categoryMapper.deleteById(id);
    }
}
