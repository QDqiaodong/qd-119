package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {

    private Long id;
    private String type;
    private Long recordId;
    private String description;
    private LocalDateTime time;
    private String productionLine;
}
