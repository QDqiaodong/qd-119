package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRequest {

    private String name;
    private String code;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private List<PackageItemRequest> items;
}
