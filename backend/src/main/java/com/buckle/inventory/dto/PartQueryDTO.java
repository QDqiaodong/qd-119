package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartQueryDTO {

    private int page = 1;
    private int size = 10;
    private String name;
    private String model;
    private String shelfPosition;
}
