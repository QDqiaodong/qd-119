package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundRequest {

    private Long partId;
    private Integer quantity;
    private String productionLine;
    private String operator;
}
