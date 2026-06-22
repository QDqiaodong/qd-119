package com.buckle.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buckle.inventory.entity.PackageInboundDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PackageInboundDetailMapper extends BaseMapper<PackageInboundDetail> {

    @Select("SELECT * FROM package_inbound_detail WHERE record_id = #{recordId}")
    List<PackageInboundDetail> selectByRecordId(@Param("recordId") Long recordId);
}
