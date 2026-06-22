package com.buckle.inventory.service;

import com.buckle.inventory.entity.PackagingMachine;

import java.util.List;

public interface PackagingMachineService {

    List<PackagingMachine> listAll();

    List<PackagingMachine> listByProductionLine(String productionLine);

    PackagingMachine getById(Long id);

    PackagingMachine getByCode(String machineCode);
}
