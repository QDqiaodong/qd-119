package com.buckle.inventory.controller;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PartDeletionCheckDTO;
import com.buckle.inventory.dto.PartQueryDTO;
import com.buckle.inventory.dto.Result;
import com.buckle.inventory.entity.Part;
import com.buckle.inventory.service.PartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@CrossOrigin
public class PartController {

    @Autowired
    private PartService partService;

    @GetMapping
    public Result<PageResult<Part>> listParts(PartQueryDTO query) {
        return Result.ok(partService.listParts(query));
    }

    @PostMapping
    public Result<Part> addPart(@RequestBody Part part) {
        return Result.ok(partService.addPart(part));
    }

    @PutMapping("/{id}")
    public Result<Part> updatePart(@PathVariable Long id, @RequestBody Part part) {
        part.setId(id);
        return Result.ok(partService.updatePart(part));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePart(@PathVariable Long id) {
        partService.deletePart(id);
        return Result.ok(null);
    }

    @GetMapping("/{id}/deletion-check")
    public Result<PartDeletionCheckDTO> checkDeletionAllowed(@PathVariable Long id) {
        return Result.ok(partService.checkDeletionAllowed(id));
    }

    @GetMapping("/{id}")
    public Result<Part> getPartById(@PathVariable Long id) {
        return Result.ok(partService.getPartById(id));
    }

    @PostMapping("/batch")
    public Result<List<Part>> batchAddParts(@RequestBody List<Part> parts) {
        return Result.ok(partService.batchAddParts(parts));
    }
}
