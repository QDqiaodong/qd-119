package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.BuckleBracketService;
import com.buckle.inventory.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BuckleBracketServiceImpl implements BuckleBracketService {

    private static final String BUCKLE_CATEGORY_CODE = "BUCKLE";
    private static final String BRACKET_CATEGORY_CODE = "BRACKET";

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public List<BucklePartDTO> listBuckles() {
        List<BucklePartDTO> cached = redisCacheService.getBucklesFromCache();
        if (cached != null) {
            return cached;
        }
        List<BucklePartDTO> result = buildParts(BUCKLE_CATEGORY_CODE, false).stream()
                .map(dto -> (BucklePartDTO) dto)
                .collect(Collectors.toList());
        redisCacheService.setBucklesCache(result);
        return result;
    }

    @Override
    public List<BracketPartDTO> listBrackets() {
        List<BracketPartDTO> cached = redisCacheService.getBracketsFromCache();
        if (cached != null) {
            return cached;
        }
        List<BracketPartDTO> result = buildParts(BRACKET_CATEGORY_CODE, true).stream()
                .map(dto -> (BracketPartDTO) dto)
                .collect(Collectors.toList());
        redisCacheService.setBracketsCache(result);
        return result;
    }

    private List<BucklePartDTO> buildParts(String categoryCode, boolean isBracket) {
        Long categoryId = resolveCategoryId(categoryCode);
        if (categoryId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getCategoryId, categoryId)
                .ne(Part::getDeleted, 1)
                .orderByAsc(Part::getModel);
        List<Part> parts = partMapper.selectList(wrapper);
        if (parts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> partIds = parts.stream().map(Part::getId).collect(Collectors.toList());

        Map<Long, LocalDateTime> lastInboundMap = loadLastInboundTime(partIds);
        Map<Long, List<String>> machinesMap = loadCompatibleMachines(partIds);

        List<BucklePartDTO> result = new ArrayList<>();
        for (Part part : parts) {
            BucklePartDTO dto = isBracket ? new BracketPartDTO() : new BucklePartDTO();
            dto.setId(part.getId());
            dto.setCategoryId(part.getCategoryId());
            dto.setName(part.getName());
            dto.setModel(part.getModel());
            dto.setTotalQuantity(part.getTotalQuantity());
            dto.setCurrentStock(part.getCurrentStock());
            dto.setShelfPosition(part.getShelfPosition());
            dto.setUpdatedAt(part.getUpdatedAt());
            dto.setLastInboundTime(lastInboundMap.get(part.getId()));
            dto.setCompatibleMachines(machinesMap.getOrDefault(part.getId(), Collections.emptyList()));
            if (isBracket) {
                int[] dimensions = parseBracketDimensions(part.getModel());
                ((BracketPartDTO) dto).setLength(dimensions[0]);
                ((BracketPartDTO) dto).setHoleSpacing(dimensions[1]);
            }
            result.add(dto);
        }
        return result;
    }

    private Long resolveCategoryId(String code) {
        AccessoryCategory category = categoryMapper.selectOne(
                new LambdaQueryWrapper<AccessoryCategory>().eq(AccessoryCategory::getCode, code));
        return category != null ? category.getId() : null;
    }

    private Map<Long, LocalDateTime> loadLastInboundTime(List<Long> partIds) {
        List<InboundRecord> records = inboundRecordMapper.selectList(
                new LambdaQueryWrapper<InboundRecord>()
                        .in(InboundRecord::getPartId, partIds)
                        .orderByDesc(InboundRecord::getCreatedAt));
        Map<Long, LocalDateTime> map = new LinkedHashMap<>();
        for (InboundRecord record : records) {
            map.putIfAbsent(record.getPartId(), record.getCreatedAt());
        }
        return map;
    }

    private Map<Long, List<String>> loadCompatibleMachines(List<Long> partIds) {
        List<OutboundRecord> records = outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>()
                        .in(OutboundRecord::getPartId, partIds));
        Map<Long, Set<String>> grouped = new LinkedHashMap<>();
        for (OutboundRecord record : records) {
            String line = record.getProductionLine();
            if (line != null && !line.trim().isEmpty()) {
                grouped.computeIfAbsent(record.getPartId(), k -> new LinkedHashSet<>()).add(line.trim());
            }
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        grouped.forEach((partId, machines) -> result.put(partId, new ArrayList<>(machines)));
        return result;
    }

    private int[] parseBracketDimensions(String model) {
        if (model == null || model.isEmpty()) {
            return new int[]{0, 0};
        }
        String[] segments = model.split("-");
        if (segments.length >= 3) {
            try {
                int length = Integer.parseInt(segments[segments.length - 2].trim());
                int holeSpacing = Integer.parseInt(segments[segments.length - 1].trim());
                return new int[]{length, holeSpacing};
            } catch (NumberFormatException ignored) {
                return new int[]{0, 0};
            }
        }
        return new int[]{0, 0};
    }
}
