package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.ScrapRequest;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.ScrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ScrapServiceImpl implements ScrapService {

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private PartMapper partMapper;

    @Override
    public PageResult<ScrapRecord> listScrap(int page, int size) {
        Page<ScrapRecord> pageParam = new Page<>(page, size);
        Page<ScrapRecord> result = scrapRecordMapper.selectPage(pageParam,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScrapRecord>()
                        .orderByDesc(ScrapRecord::getCreatedAt));
        PageResult<ScrapRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (ScrapRecord record : pageResult.getList()) {
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
    public ScrapRecord addScrap(ScrapRequest request) {
        Part part = partMapper.selectById(request.getPartId());
        if (part == null) {
            throw new RuntimeException("配件不存在");
        }
        if (part.getCurrentStock() < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前库存: " + part.getCurrentStock());
        }

        part.setCurrentStock(part.getCurrentStock() - request.getQuantity());
        part.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(part);

        ScrapRecord record = new ScrapRecord();
        record.setPartId(request.getPartId());
        record.setQuantity(request.getQuantity());
        record.setReason(request.getReason());
        record.setRemark(request.getRemark());
        record.setOperator(request.getOperator());
        record.setCreatedAt(LocalDateTime.now());
        record.setPartName(part.getName());
        record.setPartModel(part.getModel());
        scrapRecordMapper.insert(record);

        return record;
    }
}
