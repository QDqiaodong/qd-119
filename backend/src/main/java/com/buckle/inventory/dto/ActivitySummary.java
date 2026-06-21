package com.buckle.inventory.dto;

import java.util.List;

public class ActivitySummary {

    private Long partId;
    private String partName;
    private String partModel;
    private String categoryName;
    private String shelfPosition;
    private String productionLine;
    private Long inboundCount;
    private Long outboundCount;
    private Long scrapCount;
    private Long checkCount;
    private Integer totalInboundQuantity;
    private Integer totalOutboundQuantity;
    private Integer totalScrapQuantity;
    private List<ActivityEvent> recentEvents;

    public ActivitySummary() {
    }

    public ActivitySummary(Long partId, String partName, String partModel, String categoryName,
                           String shelfPosition, String productionLine, Long inboundCount, Long outboundCount,
                           Long scrapCount, Long checkCount, Integer totalInboundQuantity, Integer totalOutboundQuantity,
                           Integer totalScrapQuantity, List<ActivityEvent> recentEvents) {
        this.partId = partId;
        this.partName = partName;
        this.partModel = partModel;
        this.categoryName = categoryName;
        this.shelfPosition = shelfPosition;
        this.productionLine = productionLine;
        this.inboundCount = inboundCount;
        this.outboundCount = outboundCount;
        this.scrapCount = scrapCount;
        this.checkCount = checkCount;
        this.totalInboundQuantity = totalInboundQuantity;
        this.totalOutboundQuantity = totalOutboundQuantity;
        this.totalScrapQuantity = totalScrapQuantity;
        this.recentEvents = recentEvents;
    }

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getPartModel() { return partModel; }
    public void setPartModel(String partModel) { this.partModel = partModel; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getShelfPosition() { return shelfPosition; }
    public void setShelfPosition(String shelfPosition) { this.shelfPosition = shelfPosition; }
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    public Long getInboundCount() { return inboundCount; }
    public void setInboundCount(Long inboundCount) { this.inboundCount = inboundCount; }
    public Long getOutboundCount() { return outboundCount; }
    public void setOutboundCount(Long outboundCount) { this.outboundCount = outboundCount; }
    public Long getScrapCount() { return scrapCount; }
    public void setScrapCount(Long scrapCount) { this.scrapCount = scrapCount; }
    public Long getCheckCount() { return checkCount; }
    public void setCheckCount(Long checkCount) { this.checkCount = checkCount; }
    public Integer getTotalInboundQuantity() { return totalInboundQuantity; }
    public void setTotalInboundQuantity(Integer totalInboundQuantity) { this.totalInboundQuantity = totalInboundQuantity; }
    public Integer getTotalOutboundQuantity() { return totalOutboundQuantity; }
    public void setTotalOutboundQuantity(Integer totalOutboundQuantity) { this.totalOutboundQuantity = totalOutboundQuantity; }
    public Integer getTotalScrapQuantity() { return totalScrapQuantity; }
    public void setTotalScrapQuantity(Integer totalScrapQuantity) { this.totalScrapQuantity = totalScrapQuantity; }
    public List<ActivityEvent> getRecentEvents() { return recentEvents; }
    public void setRecentEvents(List<ActivityEvent> recentEvents) { this.recentEvents = recentEvents; }
}
