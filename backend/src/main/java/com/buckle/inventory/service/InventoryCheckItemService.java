package com.buckle.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.buckle.inventory.entity.InventoryCheckItem;

import java.util.List;

public interface InventoryCheckItemService extends IService<InventoryCheckItem> {

    boolean saveBatch(List<InventoryCheckItem> items);
}
