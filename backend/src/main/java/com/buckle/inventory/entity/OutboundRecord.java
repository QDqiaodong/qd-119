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
@TableName("outbound_record")
public class OutboundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("part_id")
    private Long partId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("production_line")
    private String productionLine;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("part_name")
    private String partName;

    @TableField("part_model")
    private String partModel;
}
