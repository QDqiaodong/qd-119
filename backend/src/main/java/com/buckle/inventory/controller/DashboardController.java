package com.buckle.inventory.controller;

import com.buckle.inventory.dto.DashboardOverview;
import com.buckle.inventory.dto.RecentActivity;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardOverview> getOverview() {
        return Result.ok(dashboardService.getOverview());
    }

    @GetMapping("/recent")
    public Result<List<RecentActivity>> getRecentActivities() {
        return Result.ok(dashboardService.getRecentActivities());
    }
}
