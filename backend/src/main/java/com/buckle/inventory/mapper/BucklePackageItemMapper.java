package com.buckle.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buckle.inventory.entity.BucklePackageItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BucklePackageItemMapper extends BaseMapper<BucklePackageItem> {

    @Select("SELECT i.*, p.name as partName, p.model as partModel, p.current_stock as currentStock, p.shelf_position as shelfPosition " +
            "FROM buckle_package_item i " +
            "LEFT JOIN part p ON i.part_id = p.id " +
            "WHERE i.package_id = #{packageId}")
    List<BucklePackageItem> selectByPackageIdWithPartInfo(@Param("packageId") Long packageId);

    @Delete("DELETE FROM buckle_package_item WHERE package_id = #{packageId}")
    int deleteByPackageId(@Param("packageId") Long packageId);
}
