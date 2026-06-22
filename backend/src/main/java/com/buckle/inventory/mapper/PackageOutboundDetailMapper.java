package com.buckle.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buckle.inventory.entity.PackageOutboundDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PackageOutboundDetailMapper extends BaseMapper<PackageOutboundDetail> {

    @Select("SELECT * FROM package_outbound_detail WHERE record_id = #{recordId}")
    List<PackageOutboundDetail> selectByRecordId(@Param("recordId") Long recordId);
}
