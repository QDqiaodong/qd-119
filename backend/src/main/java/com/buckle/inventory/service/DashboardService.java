package com.buckle.inventory.service;

import com.buckle.inventory.dto.DashboardOverview;
import com.buckle.inventory.dto.RecentActivity;

import java.util.List;

public interface DashboardService {

    DashboardOverview getOverview();

    List<RecentActivity> getRecentActivities();
}
