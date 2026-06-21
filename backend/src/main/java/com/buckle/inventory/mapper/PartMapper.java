package com.buckle.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.buckle.inventory.entity.Part;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PartMapper extends BaseMapper<Part> {

    @Update("UPDATE part SET current_stock = current_stock - #{quantity}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{partId} AND deleted = 0 AND current_stock >= #{quantity}")
    int deductStock(@Param("partId") Long partId,
                    @Param("quantity") int quantity,
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE part SET current_stock = current_stock + #{quantity}, " +
            "total_quantity = total_quantity + #{quantity}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{partId} AND deleted = 0")
    int addStock(@Param("partId") Long partId,
                 @Param("quantity") int quantity,
                 @Param("updatedAt") LocalDateTime updatedAt);
}
