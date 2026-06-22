package com.buckle.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.buckle.inventory.entity.PackagingMachine;
import com.buckle.inventory.mapper.PackagingMachineMapper;
import com.buckle.inventory.service.PackagingMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PackagingMachineServiceImpl implements PackagingMachineService {

    @Autowired
    private PackagingMachineMapper packagingMachineMapper;

    @Override
    public List<PackagingMachine> listAll() {
        LambdaQueryWrapper<PackagingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PackagingMachine::getStatus, 1);
        wrapper.orderByAsc(PackagingMachine::getSortOrder);
        wrapper.orderByAsc(PackagingMachine::getMachineCode);
        return packagingMachineMapper.selectList(wrapper);
    }

    @Override
    public List<PackagingMachine> listByProductionLine(String productionLine) {
        LambdaQueryWrapper<PackagingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PackagingMachine::getStatus, 1);
        if (StringUtils.hasText(productionLine)) {
            wrapper.eq(PackagingMachine::getProductionLine, productionLine);
        }
        wrapper.orderByAsc(PackagingMachine::getSortOrder);
        wrapper.orderByAsc(PackagingMachine::getMachineCode);
        return packagingMachineMapper.selectList(wrapper);
    }

    @Override
    public PackagingMachine getById(Long id) {
        if (id == null) {
            return null;
        }
        return packagingMachineMapper.selectById(id);
    }

    @Override
    public PackagingMachine getByCode(String machineCode) {
        if (!StringUtils.hasText(machineCode)) {
            return null;
        }
        LambdaQueryWrapper<PackagingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PackagingMachine::getMachineCode, machineCode);
        return packagingMachineMapper.selectOne(wrapper);
    }
}
