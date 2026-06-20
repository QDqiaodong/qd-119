package com.buckle.inventory.service;

import com.buckle.inventory.entity.AccessoryCategory;

import java.util.List;

public interface AccessoryCategoryService {

    List<AccessoryCategory> listAll();

    AccessoryCategory getById(Long id);

    AccessoryCategory add(AccessoryCategory category);

    AccessoryCategory update(AccessoryCategory category);

    void delete(Long id);
}
