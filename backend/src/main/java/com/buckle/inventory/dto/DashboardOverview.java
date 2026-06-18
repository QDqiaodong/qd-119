package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {

    private Long totalParts;
    private Integer totalStock;
    private Integer monthlyInbound;
    private Integer monthlyOutbound;
}
