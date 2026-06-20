package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.InventoryCheckRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InventoryCheck;
import com.buckle.inventory.entity.InventoryCheckItem;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.InventoryCheckItemMapper;
import com.buckle.inventory.mapper.InventoryCheckMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.InventoryCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryCheckServiceImpl implements InventoryCheckService {

    @Autowired
    private InventoryCheckMapper inventoryCheckMapper;

    @Autowired
    private InventoryCheckItemMapper inventoryCheckItemMapper;

    @Autowired
    private PartMapper partMapper;

    @Override
    public PageResult<InventoryCheck> listChecks(int page, int size) {
        Page<InventoryCheck> pageParam = new Page<>(page, size);
        Page<InventoryCheck> result = inventoryCheckMapper.selectPage(pageParam,
                new LambdaQueryWrapper<InventoryCheck>().orderByDesc(InventoryCheck::getCreatedAt));
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public InventoryCheck addCheck(InventoryCheckRequest request) {
        InventoryCheck check = new InventoryCheck();
        check.setQuarter(request.getQuarter());
        check.setOperator(request.getOperator());
        check.setCreatedAt(LocalDateTime.now());

        int matchCount = 0;
        int diffCount = 0;
        List<InventoryCheckItem> items = new ArrayList<>();

        for (InventoryCheckRequest.CheckItemRequest itemReq : request.getItems()) {
            Part part = partMapper.selectById(itemReq.getPartId());
            int bookQuantity = (part != null) ? part.getCurrentStock() : 0;
            int difference = itemReq.getActualQuantity() - bookQuantity;

            if (difference == 0) {
                matchCount++;
            } else {
                diffCount++;
            }

            InventoryCheckItem item = new InventoryCheckItem();
            item.setPartId(itemReq.getPartId());
            item.setBookQuantity(bookQuantity);
            item.setActualQuantity(itemReq.getActualQuantity());
            item.setDifference(difference);
            if (part != null) {
                item.setPartName(part.getName());
                item.setPartModel(part.getModel());
                item.setShelfPosition(part.getShelfPosition());
            }
            items.add(item);
        }

        check.setTotalCount(request.getItems().size());
        check.setMatchCount(matchCount);
        check.setDiffCount(diffCount);
        inventoryCheckMapper.insert(check);

        for (InventoryCheckItem item : items) {
            item.setCheckId(check.getId());
            inventoryCheckItemMapper.insert(item);
        }

        check.setItems(items);
        return check;
    }

    @Override
    public InventoryCheck getCheckDetail(Long id) {
        InventoryCheck check = inventoryCheckMapper.selectById(id);
        if (check != null) {
            List<InventoryCheckItem> items = inventoryCheckItemMapper.selectList(
                    new LambdaQueryWrapper<InventoryCheckItem>().eq(InventoryCheckItem::getCheckId, id));
            for (InventoryCheckItem item : items) {
                if (item.getPartName() == null || item.getPartModel() == null || item.getShelfPosition() == null) {
                    Part part = partMapper.selectById(item.getPartId());
                    if (part != null) {
                        if (item.getPartName() == null) {
                            item.setPartName(part.getName());
                        }
                        if (item.getPartModel() == null) {
                            item.setPartModel(part.getModel());
                        }
                        if (item.getShelfPosition() == null) {
                            item.setShelfPosition(part.getShelfPosition());
                        }
                    } else {
                        if (item.getPartName() == null) {
                            item.setPartName("未知配件");
                        }
                        if (item.getPartModel() == null) {
                            item.setPartModel("-");
                        }
                        if (item.getShelfPosition() == null) {
                            item.setShelfPosition("-");
                        }
                    }
                }
            }
            check.setItems(items);
        }
        return check;
    }
}
