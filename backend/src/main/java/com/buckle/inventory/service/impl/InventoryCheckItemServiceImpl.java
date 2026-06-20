package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.buckle.inventory.entity.InventoryCheckItem;
import com.buckle.inventory.mapper.InventoryCheckItemMapper;
import com.buckle.inventory.service.InventoryCheckItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryCheckItemServiceImpl extends ServiceImpl<InventoryCheckItemMapper, InventoryCheckItem> implements InventoryCheckItemService {

    @Override
    public boolean saveBatch(List<InventoryCheckItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return super.saveBatch(items, 1000);
    }
}
