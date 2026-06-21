package com.buckle.inventory.service.impl;

import com.buckle.inventory.dto.ActivityEvent;
import com.buckle.inventory.dto.DashboardOverview;
import com.buckle.inventory.dto.RecentActivity;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.ActivityService;
import com.buckle.inventory.service.DashboardService;
import com.buckle.inventory.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private InboundRecordMapper inboundRecordMapper;

    @Autowired
    private OutboundRecordMapper outboundRecordMapper;

    @Autowired
    private ScrapRecordMapper scrapRecordMapper;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public DashboardOverview getOverview() {
        try {
            DashboardOverview cached = redisCacheService.getDashboardOverviewFromCache();
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[getOverview] cache read failed: {}", e.getMessage());
        }

        long totalParts = 0L;
        int totalStock = 0;
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Part> partWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            partWrapper.ne(Part::getDeleted, 1);
            Long cnt = partMapper.selectCount(partWrapper);
            totalParts = cnt != null ? cnt : 0L;

            List<Part> allParts = partMapper.selectList(partWrapper);
            if (allParts != null) {
                totalStock = allParts.stream()
                        .filter(p -> p != null && p.getCurrentStock() != null)
                        .mapToInt(Part::getCurrentStock).sum();
            }
        } catch (Exception e) {
            log.warn("[getOverview] query parts failed: {}", e.getMessage());
        }

        int monthlyInbound = 0;
        int monthlyOutbound = 0;
        try {
            LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
            LocalDateTime monthEnd = LocalDateTime.now();

            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundRecord> inboundWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            inboundWrapper.between(InboundRecord::getCreatedAt, monthStart, monthEnd);
            List<InboundRecord> inboundRecords = inboundRecordMapper.selectList(inboundWrapper);
            if (inboundRecords != null) {
                monthlyInbound = inboundRecords.stream()
                        .filter(r -> r != null && r.getQuantity() != null)
                        .mapToInt(InboundRecord::getQuantity).sum();
            }

            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundRecord> outboundWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            outboundWrapper.between(OutboundRecord::getCreatedAt, monthStart, monthEnd);
            List<OutboundRecord> outboundRecords = outboundRecordMapper.selectList(outboundWrapper);
            if (outboundRecords != null) {
                monthlyOutbound = outboundRecords.stream()
                        .filter(r -> r != null && r.getQuantity() != null)
                        .mapToInt(OutboundRecord::getQuantity).sum();
            }
        } catch (Exception e) {
            log.warn("[getOverview] query monthly records failed: {}", e.getMessage());
        }

        DashboardOverview overview = new DashboardOverview(totalParts, totalStock, monthlyInbound, monthlyOutbound);
        try {
            redisCacheService.setDashboardOverviewCache(overview);
        } catch (Exception e) {
            log.warn("[getOverview] cache write failed: {}", e.getMessage());
        }
        return overview;
    }

    @Override
    public List<RecentActivity> getRecentActivities() {
        try {
            List<ActivityEvent> events = activityService.getRecentActivities(10);
            List<RecentActivity> activities = new ArrayList<>();
            if (events != null) {
                for (ActivityEvent event : events) {
                    if (event == null || event.getType() == null) continue;
                    String type;
                    switch (event.getType()) {
                        case INBOUND:
                            type = "INBOUND";
                            break;
                        case OUTBOUND:
                            type = "OUTBOUND";
                            break;
                        case SCRAP:
                            type = "SCRAP";
                            break;
                        case INVENTORY_CHECK:
                            type = "INVENTORY_CHECK";
                            break;
                        default:
                            type = event.getType().name();
                    }
                    activities.add(new RecentActivity(type, event.getDescription(), event.getTime(), event.getProductionLine()));
                }
            }
            return activities;
        } catch (Exception e) {
            log.warn("[getRecentActivities] failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
