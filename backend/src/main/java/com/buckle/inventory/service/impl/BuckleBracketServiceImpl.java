package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.dto.PageResult;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BuckleBracketServiceImpl.class);

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
        try {
            List<BucklePartDTO> cached = redisCacheService.getBucklesFromCache();
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[listBuckles] cache read failed: {}", e.getMessage());
        }
        List<BucklePartDTO> result;
        try {
            result = buildParts(BUCKLE_CATEGORY_CODE, false).stream()
                    .map(dto -> (BucklePartDTO) dto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[listBuckles] buildParts failed: {}", e.getMessage());
            return Collections.emptyList();
        }
        try {
            redisCacheService.setBucklesCache(result);
        } catch (Exception e) {
            log.warn("[listBuckles] cache write failed: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<BracketPartDTO> listBrackets() {
        try {
            List<BracketPartDTO> cached = redisCacheService.getBracketsFromCache();
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[listBrackets] cache read failed: {}", e.getMessage());
        }
        List<BracketPartDTO> result;
        try {
            result = buildParts(BRACKET_CATEGORY_CODE, true).stream()
                    .map(dto -> (BracketPartDTO) dto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[listBrackets] buildParts failed: {}", e.getMessage());
            return Collections.emptyList();
        }
        try {
            redisCacheService.setBracketsCache(result);
        } catch (Exception e) {
            log.warn("[listBrackets] cache write failed: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public PageResult<BucklePartDTO> pageBuckles(int page, int size) {
        List<BucklePartDTO> all = listBuckles();
        return paginate(all, page, size);
    }

    @Override
    public PageResult<BracketPartDTO> pageBrackets(int page, int size) {
        List<BracketPartDTO> all = listBrackets();
        return paginate(all, page, size);
    }

    @SuppressWarnings("unchecked")
    private <T> PageResult<T> paginate(List<? extends T> all, int page, int size) {
        int total = all.size();
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(total, fromIndex + size);
        List<T> pageList = fromIndex >= total ? Collections.emptyList() : new ArrayList<>(all.subList(fromIndex, toIndex));
        return new PageResult<>(pageList, total, page, size);
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
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> partIds = parts.stream()
                .filter(p -> p != null && p.getId() != null)
                .map(Part::getId)
                .collect(Collectors.toList());
        if (partIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, LocalDateTime> lastInboundMap = loadLastInboundTime(partIds);
        Map<Long, List<String>> machinesMap = loadCompatibleMachines(partIds);

        List<BucklePartDTO> result = new ArrayList<>();
        for (Part part : parts) {
            if (part == null) continue;
            try {
                BucklePartDTO dto = isBracket ? new BracketPartDTO() : new BucklePartDTO();
                dto.setId(part.getId());
                dto.setCategoryId(part.getCategoryId());
                dto.setName(part.getName());
                dto.setModel(part.getModel());
                dto.setTotalQuantity(part.getTotalQuantity() != null ? part.getTotalQuantity() : 0);
                dto.setCurrentStock(part.getCurrentStock() != null ? part.getCurrentStock() : 0);
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
            } catch (Exception e) {
                log.warn("[buildParts] map part failed, partId={}: {}", part.getId(), e.getMessage());
            }
        }
        return result;
    }

    private Long resolveCategoryId(String code) {
        try {
            AccessoryCategory category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<AccessoryCategory>().eq(AccessoryCategory::getCode, code));
            return category != null ? category.getId() : null;
        } catch (Exception e) {
            log.warn("[resolveCategoryId] code={} failed: {}", code, e.getMessage());
            return null;
        }
    }

    private Map<Long, LocalDateTime> loadLastInboundTime(List<Long> partIds) {
        try {
            List<InboundRecord> records = inboundRecordMapper.selectList(
                    new LambdaQueryWrapper<InboundRecord>()
                            .in(InboundRecord::getPartId, partIds)
                            .orderByDesc(InboundRecord::getCreatedAt));
            Map<Long, LocalDateTime> map = new LinkedHashMap<>();
            if (records != null) {
                for (InboundRecord record : records) {
                    if (record == null || record.getPartId() == null) continue;
                    map.putIfAbsent(record.getPartId(), record.getCreatedAt());
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("[loadLastInboundTime] failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, List<String>> loadCompatibleMachines(List<Long> partIds) {
        try {
            List<OutboundRecord> records = outboundRecordMapper.selectList(
                    new LambdaQueryWrapper<OutboundRecord>()
                            .in(OutboundRecord::getPartId, partIds));
            Map<Long, Set<String>> grouped = new LinkedHashMap<>();
            if (records != null) {
                for (OutboundRecord record : records) {
                    if (record == null || record.getPartId() == null) continue;
                    String machineCode = record.getMachineCode();
                    if (machineCode != null && !machineCode.trim().isEmpty()) {
                        grouped.computeIfAbsent(record.getPartId(), k -> new LinkedHashSet<>()).add(machineCode.trim());
                    }
                }
            }
            Map<Long, List<String>> result = new LinkedHashMap<>();
            grouped.forEach((partId, machines) -> result.put(partId, new ArrayList<>(machines)));
            return result;
        } catch (Exception e) {
            log.warn("[loadCompatibleMachines] failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
