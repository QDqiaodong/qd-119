package com.buckle.inventory.service.impl;

import com.buckle.inventory.dto.DashboardOverview;
import com.buckle.inventory.dto.RecentActivity;
import com.buckle.inventory.entity.InboundRecord;
import com.buckle.inventory.entity.OutboundRecord;
import com.buckle.inventory.entity.ScrapRecord;
import com.buckle.inventory.mapper.InboundRecordMapper;
import com.buckle.inventory.mapper.OutboundRecordMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.mapper.ScrapRecordMapper;
import com.buckle.inventory.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Override
    public DashboardOverview getOverview() {
        Long totalParts = partMapper.selectCount(null);

        List<com.buckle.inventory.entity.Part> allParts = partMapper.selectList(null);
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

        return new DashboardOverview(totalParts, totalStock, monthlyInbound, monthlyOutbound);
    }

    @Override
    public List<RecentActivity> getRecentActivities() {
        List<RecentActivity> activities = new ArrayList<>();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundRecord> inboundWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        inboundWrapper.orderByDesc(InboundRecord::getCreatedAt).last("LIMIT 5");
        List<InboundRecord> inboundRecords = inboundRecordMapper.selectList(inboundWrapper);
        for (InboundRecord r : inboundRecords) {
            com.buckle.inventory.entity.Part part = partMapper.selectById(r.getPartId());
            String desc = (part != null ? part.getName() : "未知配件") + " 入库 " + r.getQuantity() + "个";
            activities.add(new RecentActivity("INBOUND", desc, r.getCreatedAt()));
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundRecord> outboundWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        outboundWrapper.orderByDesc(OutboundRecord::getCreatedAt).last("LIMIT 5");
        List<OutboundRecord> outboundRecords = outboundRecordMapper.selectList(outboundWrapper);
        for (OutboundRecord r : outboundRecords) {
            com.buckle.inventory.entity.Part part = partMapper.selectById(r.getPartId());
            String desc = (part != null ? part.getName() : "未知配件") + " 出库 " + r.getQuantity() + "个 -> " + r.getProductionLine();
            activities.add(new RecentActivity("OUTBOUND", desc, r.getCreatedAt()));
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScrapRecord> scrapWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        scrapWrapper.orderByDesc(ScrapRecord::getCreatedAt).last("LIMIT 5");
        List<ScrapRecord> scrapRecords = scrapRecordMapper.selectList(scrapWrapper);
        for (ScrapRecord r : scrapRecords) {
            com.buckle.inventory.entity.Part part = partMapper.selectById(r.getPartId());
            String desc = (part != null ? part.getName() : "未知配件") + " 报废 " + r.getQuantity() + "个";
            activities.add(new RecentActivity("SCRAP", desc, r.getCreatedAt()));
        }

        activities.sort(Comparator.comparing(RecentActivity::getTime).reversed());
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }
        return activities;
    }
}
