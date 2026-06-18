package com.buckle.inventory.service;

import com.buckle.inventory.dto.InboundRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InboundRecord;

public interface InboundService {

    PageResult<InboundRecord> listInbound(int page, int size, String keyword);

    InboundRecord addInbound(InboundRequest request);
}
