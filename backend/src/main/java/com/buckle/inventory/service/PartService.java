package com.buckle.inventory.service;

import com.buckle.inventory.dto.PageResult;
import com.buckle.inventory.dto.PartDeletionCheckDTO;
import com.buckle.inventory.dto.PartQueryDTO;
import com.buckle.inventory.entity.Part;

import java.util.List;

public interface PartService {

    PageResult<Part> listParts(PartQueryDTO query);

    Part getPartById(Long id);

    Part addPart(Part part);

    Part updatePart(Part part);

    PartDeletionCheckDTO deletePart(Long id);

    PartDeletionCheckDTO checkDeletionAllowed(Long id);

    List<Part> batchAddParts(List<Part> parts);

    List<Part> getAllParts();
}
