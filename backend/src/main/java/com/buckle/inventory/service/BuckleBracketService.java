package com.buckle.inventory.service;

import com.buckle.inventory.dto.BracketPartDTO;
import com.buckle.inventory.dto.BucklePartDTO;
import com.buckle.inventory.dto.PageResult;

import java.util.List;

public interface BuckleBracketService {

    List<BucklePartDTO> listBuckles();

    List<BracketPartDTO> listBrackets();

    PageResult<BucklePartDTO> pageBuckles(int page, int size);

    PageResult<BracketPartDTO> pageBrackets(int page, int size);
}
