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
@TableName("shelf_migration_record")
public class ShelfMigrationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("part_id")
    private Long partId;

    @TableField("part_name")
    private String partName;

    @TableField("part_model")
    private String partModel;

    @TableField("source_shelf")
    private String sourceShelf;

    @TableField("target_shelf")
    private String targetShelf;

    @TableField("quantity")
    private Integer quantity;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
