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
@TableName("package_inbound_detail")
public class PackageInboundDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("part_id")
    private Long partId;

    @TableField("part_name")
    private String partName;

    @TableField("part_model")
    private String partModel;

    @TableField("quantity")
    private Integer quantity;

    @TableField("shelf_position")
    private String shelfPosition;
}
