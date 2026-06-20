package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.dto.ShelfOccupancyInfo;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.mapper.PartMapper;
import com.buckle.inventory.service.ShelfOccupancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ShelfOccupancyServiceImpl implements ShelfOccupancyService {

    @Value("${shelf.max-part-types:10}")
    private int maxPartTypes;

    @Value("${shelf.max-stock-capacity:5000}")
    private int maxStockCapacity;

    @Autowired
    private PartMapper partMapper;

    @Override
    public ShelfOccupancyInfo getShelfOccupancy(String shelfPosition) {
        ShelfOccupancyInfo info = new ShelfOccupancyInfo();
        info.setShelfPosition(shelfPosition);
        info.setMaxPartTypes(maxPartTypes);
        info.setMaxStockCapacity(maxStockCapacity);

        if (!StringUtils.hasText(shelfPosition)) {
            info.setPartTypeCount(0);
            info.setTotalStock(0);
            return info;
        }

        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Part::getShelfPosition, shelfPosition);
        wrapper.ne(Part::getDeleted, 1);
        List<Part> parts = partMapper.selectList(wrapper);

        info.setPartTypeCount(parts.size());
        int totalStock = parts.stream()
                .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                .sum();
        info.setTotalStock(totalStock);

        return info;
    }

    @Override
    public void checkCapacity(String shelfPosition, int additionalQuantity, boolean isNewPartType) {
        if (!StringUtils.hasText(shelfPosition)) {
            return;
        }

        ShelfOccupancyInfo occupancy = getShelfOccupancy(shelfPosition);

        if (isNewPartType) {
            int newTypeCount = occupancy.getPartTypeCount() + 1;
            if (newTypeCount > maxPartTypes) {
                throw new RuntimeException(
                        String.format("货架「%s」配件种类已达上限 %d，无法新增新种类配件",
                                shelfPosition, maxPartTypes));
            }
        }

        if (additionalQuantity > 0) {
            int newTotalStock = occupancy.getTotalStock() + additionalQuantity;
            if (newTotalStock > maxStockCapacity) {
                throw new RuntimeException(
                        String.format("货架「%s」库存总量将达上限 %d，当前库存 %d，拟增加 %d 后将超出容量",
                                shelfPosition, maxStockCapacity,
                                occupancy.getTotalStock(), additionalQuantity));
            }
        }
    }

    @Override
    public int getMaxPartTypes() {
        return maxPartTypes;
    }

    @Override
    public int getMaxStockCapacity() {
        return maxStockCapacity;
    }
}
