package com.buckle.inventory.service;

import com.buckle.inventory.dto.OutboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.OutboundRecord;

public interface OutboundService {

    PageResult<OutboundRecord> listOutbound(int page, int size, String productionLine, Long machineId);

    OutboundRecord addOutbound(OutboundRequest request);
}
