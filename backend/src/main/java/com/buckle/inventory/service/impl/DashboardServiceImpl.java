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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

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
        DashboardOverview cached = redisCacheService.getDashboardOverviewFromCache();
        if (cached != null) {
            return cached;
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Part> partWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        partWrapper.ne(Part::getDeleted, 1);
        Long totalParts = partMapper.selectCount(partWrapper);

        List<Part> allParts = partMapper.selectList(partWrapper);
        int totalStock = allParts.stream().mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0).sum();

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDateTime.now();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundRecord> inboundWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        inboundWrapper.between(InboundRecord::getCreatedAt, monthStart, monthEnd);
        List<InboundRecord> inboundRecords = inboundRecordMapper.selectList(inboundWrapper);
        int monthlyInbound = inboundRecords.stream().mapToInt(InboundRecord::getQuantity).sum();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundRecord> outboundWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        outboundWrapper.between(OutboundRecord::getCreatedAt, monthStart, monthEnd);
        List<OutboundRecord> outboundRecords = outboundRecordMapper.selectList(outboundWrapper);
        int monthlyOutbound = outboundRecords.stream().mapToInt(OutboundRecord::getQuantity).sum();

        DashboardOverview overview = new DashboardOverview(totalParts, totalStock, monthlyInbound, monthlyOutbound);
        redisCacheService.setDashboardOverviewCache(overview);
        return overview;
    }

    @Override
    public List<RecentActivity> getRecentActivities() {
        List<ActivityEvent> events = activityService.getRecentActivities(10);
        List<RecentActivity> activities = new ArrayList<>();
        for (ActivityEvent event : events) {
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
        return activities;
    }
}
