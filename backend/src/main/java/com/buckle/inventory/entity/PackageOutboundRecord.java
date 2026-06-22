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
@TableName("package_outbound_record")
public class PackageOutboundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_id")
    private Long packageId;

    @TableField("package_name")
    private String packageName;

    @TableField("package_code")
    private String packageCode;

    @TableField("package_quantity")
    private Integer packageQuantity;

    @TableField("production_line")
    private String productionLine;

    @TableField("machine_id")
    private Long machineId;

    @TableField("machine_code")
    private String machineCode;

    @TableField("operator")
    private String operator;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<PackageOutboundDetail> details;
}
