package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapRequest {

    private Long partId;
    private Integer quantity;
    private String reason;
    private String remark;
    private String operator;
}
