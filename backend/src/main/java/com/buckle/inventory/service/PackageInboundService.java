package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PackageInboundRequest;
import com.buckle.inventory.entity.PackageInboundRecord;

public interface PackageInboundService {

    PageResult<PackageInboundRecord> listInbound(int page, int size, String keyword);

    PackageInboundRecord getInboundById(Long id);

    PackageInboundRecord addInbound(PackageInboundRequest request);
}
