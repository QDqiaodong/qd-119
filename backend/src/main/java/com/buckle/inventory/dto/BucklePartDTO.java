package com.buckle.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class BucklePartDTO {

    private Long id;
    private Long categoryId;
    private String name;
    private String model;
    private Integer totalQuantity;
    private Integer currentStock;
    private String shelfPosition;
    private LocalDateTime updatedAt;
    private LocalDateTime lastInboundTime;
    private List<String> compatibleMachines;
}
