package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckRequest {

    private String quarter;
    private String operator;
    private List<CheckItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItemRequest {
        private Long partId;
        private Integer actualQuantity;
    }
}
