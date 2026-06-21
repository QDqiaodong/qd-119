package com.buckle.inventory.service;

import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;

import java.util.List;

public interface BuckleBracketService {

    List<BucklePartDTO> listBuckles();

    List<BracketPartDTO> listBrackets();
}
