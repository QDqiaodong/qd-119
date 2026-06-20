package com.buckle.inventory.controller;

import com.buckle.inventory.dto.Result;
import com.buckle.inventory.dto.ShelfOccupancyInfo;
import com.buckle.inventory.service.ShelfOccupancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shelf-occupancy")
@CrossOrigin
public class ShelfOccupancyController {

    @Autowired
    private ShelfOccupancyService shelfOccupancyService;

    @GetMapping("/{shelfPosition}")
    public Result<ShelfOccupancyInfo> getShelfOccupancy(@PathVariable String shelfPosition) {
        return Result.ok(shelfOccupancyService.getShelfOccupancy(shelfPosition));
    }

    @GetMapping("/config")
    public Result<ShelfOccupancyInfo> getConfig() {
        ShelfOccupancyInfo info = new ShelfOccupancyInfo();
        info.setMaxPartTypes(shelfOccupancyService.getMaxPartTypes());
        info.setMaxStockCapacity(shelfOccupancyService.getMaxStockCapacity());
        return Result.ok(info);
    }
}
