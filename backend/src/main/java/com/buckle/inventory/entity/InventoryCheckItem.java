package com.buckle.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("inventory_check_item")
public class InventoryCheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("check_id")
    private Long checkId;

    @TableField("part_id")
    private Long partId;

    @TableField("book_quantity")
    private Integer bookQuantity;

    @TableField("actual_quantity")
    private Integer actualQuantity;

    @TableField("difference")
    private Integer difference;

    @TableField("part_name")
    private String partName;

    @TableField("part_model")
    private String partModel;

    @TableField("shelf_position")
    private String shelfPosition;
}
