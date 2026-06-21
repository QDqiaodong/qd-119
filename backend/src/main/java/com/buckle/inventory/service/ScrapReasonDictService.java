package com.buckle.inventory.service;

import com.buckle.inventory.entity.ScrapReasonDict;

import java.util.List;

public interface ScrapReasonDictService {

    List<ScrapReasonDict> listEnabled();

    List<ScrapReasonDict> listAll();

    ScrapReasonDict getById(Long id);

    ScrapReasonDict getByCode(String code);

    ScrapReasonDict add(ScrapReasonDict dict);

    ScrapReasonDict update(ScrapReasonDict dict);

    void delete(Long id);
}
