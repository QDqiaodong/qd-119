package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageInboundRequest {

    private Long packageId;
    private Integer packageQuantity;
    private String operator;
    private String remark;
}
