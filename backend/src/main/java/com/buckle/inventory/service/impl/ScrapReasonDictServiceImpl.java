package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.entity.ScrapReasonDict;
import com.buckle.inventory.mapper.ScrapReasonDictMapper;
import com.buckle.inventory.service.ScrapReasonDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScrapReasonDictServiceImpl implements ScrapReasonDictService {

    @Autowired
    private ScrapReasonDictMapper scrapReasonDictMapper;

    @Override
    public List<ScrapReasonDict> listEnabled() {
        LambdaQueryWrapper<ScrapReasonDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScrapReasonDict::getEnabled, 1)
                .orderByAsc(ScrapReasonDict::getSortOrder);
        return scrapReasonDictMapper.selectList(wrapper);
    }

    @Override
    public List<ScrapReasonDict> listAll() {
        LambdaQueryWrapper<ScrapReasonDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ScrapReasonDict::getSortOrder);
        return scrapReasonDictMapper.selectList(wrapper);
    }

    @Override
    public ScrapReasonDict getById(Long id) {
        return scrapReasonDictMapper.selectById(id);
    }

    @Override
    public ScrapReasonDict getByCode(String code) {
        LambdaQueryWrapper<ScrapReasonDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScrapReasonDict::getCode, code);
        return scrapReasonDictMapper.selectOne(wrapper);
    }

    @Override
    public ScrapReasonDict add(ScrapReasonDict dict) {
        if (dict.getSortOrder() == null) {
            dict.setSortOrder(0);
        }
        if (dict.getEnabled() == null) {
            dict.setEnabled(1);
        }
        dict.setCreatedAt(LocalDateTime.now());
        dict.setUpdatedAt(LocalDateTime.now());
        scrapReasonDictMapper.insert(dict);
        return dict;
    }

    @Override
    public ScrapReasonDict update(ScrapReasonDict dict) {
        dict.setUpdatedAt(LocalDateTime.now());
        scrapReasonDictMapper.updateById(dict);
        return dict;
    }

    @Override
    public void delete(Long id) {
        scrapReasonDictMapper.deleteById(id);
    }
}
