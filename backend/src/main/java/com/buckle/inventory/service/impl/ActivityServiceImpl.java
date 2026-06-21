package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.dto.ActivityEvent;
import com.buckle.inventory.dto.ActivitySummary;
import com.buckle.inventory.entity.AccessoryCategory;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.InventoryCheck;
import com.buckle.inventory.entity.InventoryCheckItem;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.AccessoryCategoryMapper;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.InventoryCheckItemMapper;
import com.buckle.inventory.mapper.InventoryCheckMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private InventoryCheckMapper inventoryCheckMapper;

    @Autowired
    private InventoryCheckItemMapper inventoryCheckItemMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private AccessoryCategoryMapper categoryMapper;

    @Override
    public List<ActivityEvent> getRecentActivities(int limit) {
        List<ActivityEvent> allEvents = collectAllEvents();
        allEvents.sort(Comparator.comparing(ActivityEvent::getTime).reversed());
        return allEvents.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<ActivityEvent> getActivitiesByPartId(Long partId, int limit) {
        List<ActivityEvent> events = new ArrayList<>();
        events.addAll(convertInboundToEvents(inboundRecordMapper.selectList(
                new LambdaQueryWrapper<InboundRecord>().eq(InboundRecord::getPartId, partId)
                        .orderByDesc(InboundRecord::getCreatedAt).last("LIMIT " + limit))));
        events.addAll(convertOutboundToEvents(outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getPartId, partId)
                        .orderByDesc(OutboundRecord::getCreatedAt).last("LIMIT " + limit))));
        events.addAll(convertScrapToEvents(scrapRecordMapper.selectList(
                new LambdaQueryWrapper<ScrapRecord>().eq(ScrapRecord::getPartId, partId)
                        .orderByDesc(ScrapRecord::getCreatedAt).last("LIMIT " + limit))));
        events.addAll(convertCheckItemsToEvents(inventoryCheckItemMapper.selectList(
                new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getPartId, partId)
                        .orderByDesc(InventoryCheckItem::getId).last("LIMIT " + limit))));
        events.sort(Comparator.comparing(ActivityEvent::getTime).reversed());
        return events.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<ActivityEvent> getActivitiesByProductionLine(String productionLine, int limit) {
        List<ActivityEvent> events = new ArrayList<>();
        if (StringUtils.hasText(productionLine)) {
            events.addAll(convertOutboundToEvents(outboundRecordMapper.selectList(
                    new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getProductionLine, productionLine)
                            .orderByDesc(OutboundRecord::getCreatedAt).last("LIMIT " + limit))));
        }
        events.sort(Comparator.comparing(ActivityEvent::getTime).reversed());
        return events.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<ActivityEvent> getActivitiesByTimeRange(LocalDateTime start, LocalDateTime end) {
        List<ActivityEvent> events = new ArrayList<>();
        events.addAll(convertInboundToEvents(inboundRecordMapper.selectList(
                new LambdaQueryWrapper<InboundRecord>().between(InboundRecord::getCreatedAt, start, end))));
        events.addAll(convertOutboundToEvents(outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>().between(OutboundRecord::getCreatedAt, start, end))));
        events.addAll(convertScrapToEvents(scrapRecordMapper.selectList(
                new LambdaQueryWrapper<ScrapRecord>().between(ScrapRecord::getCreatedAt, start, end))));
        events.addAll(convertCheckItemsToEvents(inventoryCheckItemMapper.selectList(
                new LambdaQueryWrapper<InventoryCheckItem>().between(InventoryCheckItem::getId, 0L, Long.MAX_VALUE))));
        events.sort(Comparator.comparing(ActivityEvent::getTime).reversed());
        return events;
    }

    @Override
    public List<ActivityEvent> getActivitiesByShelfPosition(String shelfPosition, int limit) {
        List<ActivityEvent> events = new ArrayList<>();
        if (StringUtils.hasText(shelfPosition)) {
            events.addAll(convertInboundToEvents(inboundRecordMapper.selectList(
                    new LambdaQueryWrapper<InboundRecord>().eq(InboundRecord::getShelfPosition, shelfPosition)
                            .orderByDesc(InboundRecord::getCreatedAt).last("LIMIT " + limit))));
            events.addAll(convertCheckItemsToEvents(inventoryCheckItemMapper.selectList(
                    new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getShelfPosition, shelfPosition)
                            .orderByDesc(InventoryCheckItem::getId).last("LIMIT " + limit))));
        }
        events.sort(Comparator.comparing(ActivityEvent::getTime).reversed());
        return events.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public ActivitySummary getSummaryByPartId(Long partId) {
        Part part = partMapper.selectById(partId);
        if (part == null) {
            return null;
        }
        ActivitySummary summary = new ActivitySummary();
        summary.setPartId(partId);
        summary.setPartName(part.getName());
        summary.setPartModel(part.getModel());
        summary.setShelfPosition(part.getShelfPosition());

        if (part.getCategoryId() != null) {
            AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
            if (category != null) {
                summary.setCategoryName(category.getName());
            }
        }

        List<InboundRecord> inboundRecords = inboundRecordMapper.selectList(
                new LambdaQueryWrapper<InboundRecord>().eq(InboundRecord::getPartId, partId));
        summary.setInboundCount((long) inboundRecords.size());
        summary.setTotalInboundQuantity(inboundRecords.stream().mapToInt(InboundRecord::getQuantity).sum());

        List<OutboundRecord> outboundRecords = outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getPartId, partId));
        summary.setOutboundCount((long) outboundRecords.size());
        summary.setTotalOutboundQuantity(outboundRecords.stream().mapToInt(OutboundRecord::getQuantity).sum());

        List<ScrapRecord> scrapRecords = scrapRecordMapper.selectList(
                new LambdaQueryWrapper<ScrapRecord>().eq(ScrapRecord::getPartId, partId));
        summary.setScrapCount((long) scrapRecords.size());
        summary.setTotalScrapQuantity(scrapRecords.stream().mapToInt(ScrapRecord::getQuantity).sum());

        Long checkCount = inventoryCheckItemMapper.selectCount(
                new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getPartId, partId));
        summary.setCheckCount(checkCount);

        summary.setRecentEvents(getActivitiesByPartId(partId, 10));
        return summary;
    }

    @Override
    public List<ActivitySummary> getSummaryByProductionLine(String productionLine) {
        if (!StringUtils.hasText(productionLine)) {
            return new ArrayList<>();
        }
        List<OutboundRecord> outboundRecords = outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getProductionLine, productionLine));

        Map<Long, List<OutboundRecord>> groupedByPart = outboundRecords.stream()
                .collect(Collectors.groupingBy(OutboundRecord::getPartId));

        List<ActivitySummary> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<OutboundRecord>> entry : groupedByPart.entrySet()) {
            Long partId = entry.getKey();
            List<OutboundRecord> records = entry.getValue();
            Part part = partMapper.selectById(partId);
            if (part == null) {
                continue;
            }
            ActivitySummary summary = new ActivitySummary();
            summary.setPartId(partId);
            summary.setPartName(part.getName());
            summary.setPartModel(part.getModel());
            summary.setShelfPosition(part.getShelfPosition());
            summary.setProductionLine(productionLine);
            if (part.getCategoryId() != null) {
                AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
                if (category != null) {
                    summary.setCategoryName(category.getName());
                }
            }
            summary.setOutboundCount((long) records.size());
            summary.setTotalOutboundQuantity(records.stream().mapToInt(OutboundRecord::getQuantity).sum());
            summaries.add(summary);
        }
        summaries.sort(Comparator.comparing(ActivitySummary::getTotalOutboundQuantity).reversed());
        return summaries;
    }

    @Override
    public void recordActivity(ActivityEvent event) {
    }

    private List<ActivityEvent> collectAllEvents() {
        List<ActivityEvent> events = new ArrayList<>();
        events.addAll(convertInboundToEvents(inboundRecordMapper.selectList(
                new LambdaQueryWrapper<InboundRecord>().orderByDesc(InboundRecord::getCreatedAt).last("LIMIT 50"))));
        events.addAll(convertOutboundToEvents(outboundRecordMapper.selectList(
                new LambdaQueryWrapper<OutboundRecord>().orderByDesc(OutboundRecord::getCreatedAt).last("LIMIT 50"))));
        events.addAll(convertScrapToEvents(scrapRecordMapper.selectList(
                new LambdaQueryWrapper<ScrapRecord>().orderByDesc(ScrapRecord::getCreatedAt).last("LIMIT 50"))));
        events.addAll(convertCheckItemsToEvents(inventoryCheckItemMapper.selectList(
                new LambdaQueryWrapper<InventoryCheckItem>().orderByDesc(InventoryCheckItem::getId).last("LIMIT 50"))));
        return events;
    }

    private List<ActivityEvent> convertInboundToEvents(List<InboundRecord> records) {
        List<ActivityEvent> events = new ArrayList<>();
        Map<Long, Part> partCache = new HashMap<>();
        for (InboundRecord r : records) {
            ActivityEvent event = new ActivityEvent();
            event.setType(ActivityEvent.ActivityType.INBOUND);
            event.setPartId(r.getPartId());
            event.setQuantity(r.getQuantity());
            event.setShelfPosition(r.getShelfPosition());
            event.setOperator(r.getOperator());
            event.setTime(r.getCreatedAt());
            String partName = r.getPartName();
            String partModel = r.getPartModel();
            Long categoryId = null;
            String categoryName = null;
            if (partName == null || partModel == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    partName = part.getName();
                    partModel = part.getModel();
                    categoryId = part.getCategoryId();
                }
            }
            if (categoryId == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    categoryId = part.getCategoryId();
                }
            }
            if (categoryId != null) {
                AccessoryCategory category = categoryMapper.selectById(categoryId);
                if (category != null) {
                    categoryName = category.getName();
                }
            }
            event.setPartName(partName != null ? partName : "未知配件");
            event.setPartModel(partModel != null ? partModel : "-");
            event.setCategoryId(categoryId);
            event.setCategoryName(categoryName);
            event.setDescription(event.getPartName() + " 入库 " + r.getQuantity() + "个");
            events.add(event);
        }
        return events;
    }

    private List<ActivityEvent> convertOutboundToEvents(List<OutboundRecord> records) {
        List<ActivityEvent> events = new ArrayList<>();
        Map<Long, Part> partCache = new HashMap<>();
        for (OutboundRecord r : records) {
            ActivityEvent event = new ActivityEvent();
            event.setType(ActivityEvent.ActivityType.OUTBOUND);
            event.setPartId(r.getPartId());
            event.setQuantity(r.getQuantity());
            event.setProductionLine(r.getProductionLine());
            event.setOperator(r.getOperator());
            event.setTime(r.getCreatedAt());
            String partName = r.getPartName();
            String partModel = r.getPartModel();
            Long categoryId = null;
            String categoryName = null;
            String shelfPosition = null;
            if (partName == null || partModel == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    partName = part.getName();
                    partModel = part.getModel();
                    categoryId = part.getCategoryId();
                    shelfPosition = part.getShelfPosition();
                }
            }
            if (categoryId == null || shelfPosition == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    categoryId = part.getCategoryId();
                    shelfPosition = part.getShelfPosition();
                }
            }
            if (categoryId != null) {
                AccessoryCategory category = categoryMapper.selectById(categoryId);
                if (category != null) {
                    categoryName = category.getName();
                }
            }
            event.setPartName(partName != null ? partName : "未知配件");
            event.setPartModel(partModel != null ? partModel : "-");
            event.setCategoryId(categoryId);
            event.setCategoryName(categoryName);
            event.setShelfPosition(shelfPosition);
            event.setDescription(event.getPartName() + " 出库 " + r.getQuantity() + "个");
            events.add(event);
        }
        return events;
    }

    private List<ActivityEvent> convertScrapToEvents(List<ScrapRecord> records) {
        List<ActivityEvent> events = new ArrayList<>();
        Map<Long, Part> partCache = new HashMap<>();
        for (ScrapRecord r : records) {
            ActivityEvent event = new ActivityEvent();
            event.setType(ActivityEvent.ActivityType.SCRAP);
            event.setPartId(r.getPartId());
            event.setQuantity(r.getQuantity());
            event.setOperator(r.getOperator());
            event.setTime(r.getCreatedAt());
            event.setExtra(r.getReason());
            String partName = r.getPartName();
            String partModel = r.getPartModel();
            Long categoryId = null;
            String categoryName = null;
            String shelfPosition = null;
            if (partName == null || partModel == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    partName = part.getName();
                    partModel = part.getModel();
                    categoryId = part.getCategoryId();
                    shelfPosition = part.getShelfPosition();
                }
            }
            if (categoryId == null || shelfPosition == null) {
                Part part = partCache.computeIfAbsent(r.getPartId(), id -> partMapper.selectById(id));
                if (part != null) {
                    categoryId = part.getCategoryId();
                    shelfPosition = part.getShelfPosition();
                }
            }
            if (categoryId != null) {
                AccessoryCategory category = categoryMapper.selectById(categoryId);
                if (category != null) {
                    categoryName = category.getName();
                }
            }
            event.setPartName(partName != null ? partName : "未知配件");
            event.setPartModel(partModel != null ? partModel : "-");
            event.setCategoryId(categoryId);
            event.setCategoryName(categoryName);
            event.setShelfPosition(shelfPosition);
            event.setDescription(event.getPartName() + " 报废 " + r.getQuantity() + "个, 原因: " + r.getReason());
            events.add(event);
        }
        return events;
    }

    private List<ActivityEvent> convertCheckItemsToEvents(List<InventoryCheckItem> items) {
        List<ActivityEvent> events = new ArrayList<>();
        Map<Long, InventoryCheck> checkCache = new HashMap<>();
        for (InventoryCheckItem item : items) {
            ActivityEvent event = new ActivityEvent();
            event.setType(ActivityEvent.ActivityType.INVENTORY_CHECK);
            event.setPartId(item.getPartId());
            event.setQuantity(item.getDifference());
            event.setShelfPosition(item.getShelfPosition());
            event.setTime(LocalDateTime.now());
            InventoryCheck check = checkCache.computeIfAbsent(item.getCheckId(), id -> inventoryCheckMapper.selectById(id));
            if (check != null) {
                event.setOperator(check.getOperator());
                event.setTime(check.getCreatedAt());
                event.setExtra(check.getQuarter());
            }
            String partName = item.getPartName();
            String partModel = item.getPartModel();
            if (partName == null || partModel == null) {
                Part part = partMapper.selectById(item.getPartId());
                if (part != null) {
                    partName = part.getName();
                    partModel = part.getModel();
                    event.setCategoryId(part.getCategoryId());
                    if (part.getCategoryId() != null) {
                        AccessoryCategory category = categoryMapper.selectById(part.getCategoryId());
                        if (category != null) {
                            event.setCategoryName(category.getName());
                        }
                    }
                }
            }
            event.setPartName(partName != null ? partName : "未知配件");
            event.setPartModel(partModel != null ? partModel : "-");
            String diffStr = item.getDifference() > 0 ? "+" + item.getDifference() : String.valueOf(item.getDifference());
            event.setDescription(event.getPartName() + " 盘点差异 " + diffStr + " (账面:" + item.getBookQuantity() + ", 实盘:" + item.getActualQuantity() + ")");
            events.add(event);
        }
        return events;
    }
}
