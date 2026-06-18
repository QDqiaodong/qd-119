package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.ScrapRequest;
import com.buckle.inventory.entity.ScrapRecord;

public interface ScrapService {

    PageResult<ScrapRecord> listScrap(int page, int size);

    ScrapRecord addScrap(ScrapRequest request);
}
