package com.buckle.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("inventory_check")
public class InventoryCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("quarter")
    private String quarter;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("match_count")
    private Integer matchCount;

    @TableField("diff_count")
    private Integer diffCount;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<InventoryCheckItem> items;
}
