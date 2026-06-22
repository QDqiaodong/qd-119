package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageOutboundRequest;
import com.buckle.inventory.entity.PackageOutboundRecord;

public interface PackageOutboundService {

    PageResult<PackageOutboundRecord> listOutbound(int page, int size, String productionLine, Long machineId);

    PackageOutboundRecord getOutboundById(Long id);

    PackageOutboundRecord addOutbound(PackageOutboundRequest request);
}
