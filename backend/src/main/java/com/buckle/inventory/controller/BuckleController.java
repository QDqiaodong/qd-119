package com.buckle.inventory.controller;

import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.service.BuckleBracketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buckles")
@CrossOrigin
public class BuckleController {

    @Autowired
    private BuckleBracketService buckleBracketService;

    @GetMapping
    public Result<PageResult<BucklePartDTO>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "100") int size) {
        List<BucklePartDTO> list = buckleBracketService.listBuckles();
        PageResult<BucklePartDTO> pageResult = new PageResult<>(list, list.size(), page, size);
        return Result.ok(pageResult);
    }
}
