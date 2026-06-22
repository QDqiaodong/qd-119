package com.buckle.inventory.controller;

import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.service.BuckleBracketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brackets")
@CrossOrigin
public class BracketController {

    @Autowired
    private BuckleBracketService buckleBracketService;

    @GetMapping
    public Result<PageResult<BracketPartDTO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "100") int size) {
        return Result.ok(buckleBracketService.pageBrackets(page, size));
    }
}
