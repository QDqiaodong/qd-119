package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageOutboundRequest {

    private Long packageId;
    private Integer packageQuantity;
    private String productionLine;
    private Long machineId;
    private String operator;
    private String remark;
}
