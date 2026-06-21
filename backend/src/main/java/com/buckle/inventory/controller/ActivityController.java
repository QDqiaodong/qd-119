package com.buckle.inventory.controller;

import com.buckle.inventory.dto.ActivityEvent;
import com.buckle.inventory.dto.ActivitySummary;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/recent")
    public Result<List<ActivityEvent>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(activityService.getRecentActivities(limit));
    }

    @GetMapping("/part/{partId}")
    public Result<List<ActivityEvent>> getActivitiesByPartId(
            @PathVariable Long partId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(activityService.getActivitiesByPartId(partId, limit));
    }

    @GetMapping("/part/{partId}/summary")
    public Result<ActivitySummary> getSummaryByPartId(@PathVariable Long partId) {
        return Result.ok(activityService.getSummaryByPartId(partId));
    }

    @GetMapping("/production-line/{productionLine}")
    public Result<List<ActivityEvent>> getActivitiesByProductionLine(
            @PathVariable String productionLine,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(activityService.getActivitiesByProductionLine(productionLine, limit));
    }

    @GetMapping("/production-line/{productionLine}/summary")
    public Result<List<ActivitySummary>> getSummaryByProductionLine(@PathVariable String productionLine) {
        return Result.ok(activityService.getSummaryByProductionLine(productionLine));
    }

    @GetMapping("/shelf/{shelfPosition}")
    public Result<List<ActivityEvent>> getActivitiesByShelfPosition(
            @PathVariable String shelfPosition,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(activityService.getActivitiesByShelfPosition(shelfPosition, limit));
    }

    @GetMapping("/range")
    public Result<List<ActivityEvent>> getActivitiesByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.ok(activityService.getActivitiesByTimeRange(start, end));
    }
}
