package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundRequest {

    private Long partId;
    private Integer quantity;
    private String shelfPosition;
    private String operator;
    private String partName;
    private String partModel;
}
