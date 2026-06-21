package com.buckle.inventory.dto;

import java.time.LocalDateTime;

public class ActivityEvent {

    public enum ActivityType {
        INBOUND,
        OUTBOUND,
        INVENTORY_CHECK,
        SCRAP,
        PART_EDIT,
        PART_CREATE,
        PART_DELETE
    }

    private ActivityType type;
    private Long partId;
    private String partName;
    private String partModel;
    private Long categoryId;
    private String categoryName;
    private String shelfPosition;
    private String productionLine;
    private Integer quantity;
    private String operator;
    private LocalDateTime time;
    private String description;
    private String extra;

    public ActivityEvent() {
    }

    public ActivityEvent(ActivityType type, Long partId, String partName, String partModel, Long categoryId,
                         String categoryName, String shelfPosition, String productionLine, Integer quantity,
                         String operator, LocalDateTime time, String description, String extra) {
        this.type = type;
        this.partId = partId;
        this.partName = partName;
        this.partModel = partModel;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.shelfPosition = shelfPosition;
        this.productionLine = productionLine;
        this.quantity = quantity;
        this.operator = operator;
        this.time = time;
        this.description = description;
        this.extra = extra;
    }

    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getPartModel() { return partModel; }
    public void setPartModel(String partModel) { this.partModel = partModel; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getShelfPosition() { return shelfPosition; }
    public void setShelfPosition(String shelfPosition) { this.shelfPosition = shelfPosition; }
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
