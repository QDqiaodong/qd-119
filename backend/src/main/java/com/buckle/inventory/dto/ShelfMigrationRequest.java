package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShelfMigrationRequest {

    private Long partId;
    private Integer quantity;
    private String targetShelf;
    private String operator;
}
