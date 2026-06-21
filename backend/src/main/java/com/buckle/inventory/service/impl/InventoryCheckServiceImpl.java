package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.buckle.inventory.dto.InventoryCheckRequest;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.entity.InventoryCheck;
import com.buckle.inventory.entity.InventoryCheckItem;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.InventoryCheckMapper;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.InventoryCheckItemService;
import com.buckle.inventory.service.InventoryCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InventoryCheckServiceImpl implements InventoryCheckService {

    @Autowired
    private InventoryCheckMapper inventoryCheckMapper;

    @Autowired
    private InventoryCheckItemService inventoryCheckItemService;

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
        check.setQuarter(normalizeQuarter(request.getQuarter()));
        check.setOperator(request.getOperator());
        check.setCreatedAt(LocalDateTime.now());

        int matchCount = 0;
        int diffCount = 0;
        List<InventoryCheckItem> items = new ArrayList<>();

        for (InventoryCheckRequest.CheckItemRequest itemReq : request.getItems()) {
            Part part = partMapper.selectById(itemReq.getPartId());
            if (part == null) {
                throw new RuntimeException("盘点配件不存在，无法参与盘点: partId=" + itemReq.getPartId());
            }
            if (part.getDeleted() != null && part.getDeleted() == 1) {
                throw new RuntimeException("盘点配件已删除，无法参与盘点: partId=" + itemReq.getPartId());
            }
            int bookQuantity = part.getCurrentStock() != null ? part.getCurrentStock() : 0;
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
            item.setPartName(part.getName());
            item.setPartModel(part.getModel());
            item.setShelfPosition(part.getShelfPosition());
            items.add(item);
        }

        check.setTotalCount(request.getItems().size());
        check.setMatchCount(matchCount);
        check.setDiffCount(diffCount);
        inventoryCheckMapper.insert(check);

        for (InventoryCheckItem item : items) {
            item.setCheckId(check.getId());
        }
        inventoryCheckItemService.saveBatch(items);

        check.setItems(items);
        return check;
    }

    @Override
    public InventoryCheck getCheckDetail(Long id) {
        InventoryCheck check = inventoryCheckMapper.selectById(id);
        if (check != null) {
            List<InventoryCheckItem> items = inventoryCheckItemService.list(
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

    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})[-\\s]?[Qq](\\d)$");
    private static final int START_YEAR = 2024;

    @Override
    public List<String> listAvailableQuarters() {
        List<String> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        for (int y = START_YEAR; y <= currentYear; y++) {
            int maxQ = (y == currentYear) ? currentQuarter : 4;
            for (int q = 1; q <= maxQ; q++) {
                result.add(y + "-Q" + q);
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public String normalizeQuarter(String quarter) {
        if (quarter == null || quarter.trim().isEmpty()) {
            throw new IllegalArgumentException("盘点季度不能为空");
        }
        Matcher matcher = QUARTER_PATTERN.matcher(quarter.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("季度格式不正确，应为 YYYY-QN（例如 2024-Q1）");
        }
        int year = Integer.parseInt(matcher.group(1));
        int q = Integer.parseInt(matcher.group(2));
        if (q < 1 || q > 4) {
            throw new IllegalArgumentException("季度必须是 1-4 之间的数字");
        }
        return year + "-Q" + q;
    }
}
