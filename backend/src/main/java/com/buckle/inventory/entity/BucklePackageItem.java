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
@TableName("buckle_package_item")
public class BucklePackageItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_id")
    private Long packageId;

    @TableField("part_id")
    private Long partId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String partName;

    @TableField(exist = false)
    private String partModel;

    @TableField(exist = false)
    private Integer currentStock;

    @TableField(exist = false)
    private String shelfPosition;
}
