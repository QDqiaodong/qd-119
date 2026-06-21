package com.buckle.inventory.service;

import com.buckle.inventory.dto.ActivityEvent;
import com.buckle.inventory.dto.ActivitySummary;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService {

    List<ActivityEvent> getRecentActivities(int limit);

    List<ActivityEvent> getActivitiesByPartId(Long partId, int limit);

    List<ActivityEvent> getActivitiesByProductionLine(String productionLine, int limit);

    List<ActivityEvent> getActivitiesByTimeRange(LocalDateTime start, LocalDateTime end);

    List<ActivityEvent> getActivitiesByShelfPosition(String shelfPosition, int limit);

    ActivitySummary getSummaryByPartId(Long partId);

    List<ActivitySummary> getSummaryByProductionLine(String productionLine);

    void recordActivity(ActivityEvent event);
}
