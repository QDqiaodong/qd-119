package com.buckle.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartDeletionCheckDTO {

    private boolean canDelete;

    private int inboundCount;

    private int outboundCount;

    private int scrapCount;

    private int inventoryCheckCount;

    public int getTotalRelatedCount() {
        return inboundCount + outboundCount + scrapCount + inventoryCheckCount;
    }
}
