package com.buckle.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("inbound_record")
public class InboundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("part_id")
    private Long partId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("shelf_position")
    private String shelfPosition;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String partName;

    @TableField(exist = false)
    private String partModel;
}
