package com.buckle.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {

    private Long totalParts;
    private Integer totalStock;
    private Integer monthlyInbound;
    private Integer monthlyOutbound;
    private Integer monthlyConfirmedScrap;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statPeriodStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statPeriodEnd;
}
