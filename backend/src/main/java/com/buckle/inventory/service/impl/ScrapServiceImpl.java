package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.ScrapRequest;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapReasonDict;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.RedisCacheService;
import com.buckle.inventory.service.ScrapReasonDictService;
import com.buckle.inventory.service.ScrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScrapServiceImpl implements ScrapService {

    private static final String OTHER_REASON_CODE = "OTHER";

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private ScrapReasonDictService scrapReasonDictService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public PageResult<ScrapRecord> listScrap(int page, int size) {
        Page<ScrapRecord> pageParam = new Page<>(page, size);
        Page<ScrapRecord> result = scrapRecordMapper.selectPage(pageParam,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScrapRecord>()
                        .orderByDesc(ScrapRecord::getCreatedAt));
        PageResult<ScrapRecord> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, size);
        for (ScrapRecord record : pageResult.getList()) {
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
    public ScrapRecord addScrap(ScrapRequest request) {
        validateRequest(request);

        Part part = partMapper.selectById(request.getPartId());
        if (part == null) {
            throw new RuntimeException("配件不存在");
        }
        if (part.getDeleted() != null && part.getDeleted() == 1) {
            throw new RuntimeException("配件已删除，无法报废");
        }

        int currentStockBefore = part.getCurrentStock() != null ? part.getCurrentStock() : 0;
        if (currentStockBefore < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前库存: " + currentStockBefore);
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = partMapper.deductStock(request.getPartId(), request.getQuantity(), now);
        if (affected == 0) {
            Part latest = partMapper.selectById(request.getPartId());
            int latestStock = latest != null && latest.getCurrentStock() != null ? latest.getCurrentStock() : 0;
            throw new RuntimeException("库存不足，当前可用库存: " + latestStock);
        }

        Part updatedPart = partMapper.selectById(request.getPartId());

        String processedReasons = processScrapReasons(request.getReason());

        ScrapRecord record = new ScrapRecord();
        record.setPartId(request.getPartId());
        record.setQuantity(request.getQuantity());
        record.setReason(processedReasons);
        record.setRemark(request.getRemark());
        record.setOperator(request.getOperator());
        record.setCreatedAt(now);
        record.setPartName(updatedPart.getName());
        record.setPartModel(updatedPart.getModel());
        scrapRecordMapper.insert(record);

        redisCacheService.evictPartRelatedCache(updatedPart.getId(), null, null,
                updatedPart.getShelfPosition(), updatedPart.getCategoryId());
        return record;
    }

    private String processScrapReasons(String reasons) {
        if (!StringUtils.hasText(reasons)) {
            return reasons;
        }
        List<ScrapReasonDict> allReasons = scrapReasonDictService.listAll();
        Map<String, ScrapReasonDict> reasonMap = allReasons.stream()
                .collect(Collectors.toMap(ScrapReasonDict::getName, r -> r));

        List<String> reasonList = Arrays.asList(reasons.split(","));
        Set<String> uniqueReasons = new LinkedHashSet<>(reasonList);
        List<String> sortedReasons = new ArrayList<>(uniqueReasons);
        sortedReasons.sort(Comparator.comparingInt(r -> {
            ScrapReasonDict dict = reasonMap.get(r);
            return dict != null && dict.getSortOrder() != null ? dict.getSortOrder() : Integer.MAX_VALUE;
        }));

        return String.join(",", sortedReasons);
    }

    private void validateRequest(ScrapRequest request) {
        if (request == null) {
            throw new RuntimeException("报废请求不能为空");
        }
        if (request.getQuantity() == null) {
            throw new RuntimeException("报废数量不能为空");
        }
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("报废数量必须大于0，当前值: " + request.getQuantity());
        }
        if (!StringUtils.hasText(request.getOperator())) {
            throw new RuntimeException("操作人不能为空");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new RuntimeException("报废原因不能为空");
        }
        validateScrapReasons(request.getReason(), request.getRemark());
    }

    private void validateScrapReasons(String reasons, String remark) {
        String[] reasonArray = reasons.split(",");
        if (reasonArray.length == 0) {
            throw new RuntimeException("请选择至少一个报废原因");
        }
        List<ScrapReasonDict> validReasons = scrapReasonDictService.listEnabled();
        Set<String> validReasonNames = validReasons.stream()
                .map(ScrapReasonDict::getName)
                .collect(Collectors.toSet());

        ScrapReasonDict otherReasonDict = validReasons.stream()
                .filter(r -> OTHER_REASON_CODE.equals(r.getCode()))
                .findFirst()
                .orElse(null);
        String otherReasonName = otherReasonDict != null ? otherReasonDict.getName() : null;

        boolean containsOther = false;
        for (String reason : reasonArray) {
            if (!validReasonNames.contains(reason)) {
                throw new RuntimeException("无效的报废原因: " + reason);
            }
            if (otherReasonName != null && otherReasonName.equals(reason)) {
                containsOther = true;
            }
        }

        if (containsOther && !StringUtils.hasText(remark)) {
            throw new RuntimeException("选择「其他」报废原因时，备注不能为空");
        }
    }
}
