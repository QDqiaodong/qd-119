CREATE DATABASE IF NOT EXISTS buckle_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE buckle_inventory;

CREATE TABLE IF NOT EXISTS part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '配件名称',
    model VARCHAR(100) NOT NULL COMMENT '配件型号',
    total_quantity INT NOT NULL DEFAULT 0 COMMENT '入库总量',
    current_stock INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    shelf_position VARCHAR(50) NOT NULL COMMENT '货架位置',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_model (model),
    INDEX idx_shelf (shelf_position),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB COMMENT='配件信息表';

CREATE TABLE IF NOT EXISTS inbound_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '配件ID',
    part_name VARCHAR(100) COMMENT '配件名称(快照)',
    part_model VARCHAR(100) COMMENT '配件型号(快照)',
    quantity INT NOT NULL COMMENT '入库数量',
    shelf_position VARCHAR(50) COMMENT '货架位置',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='入库流水表';

CREATE TABLE IF NOT EXISTS outbound_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '配件ID',
    part_name VARCHAR(100) COMMENT '配件名称(快照)',
    part_model VARCHAR(100) COMMENT '配件型号(快照)',
    quantity INT NOT NULL COMMENT '出库数量',
    production_line VARCHAR(50) NOT NULL COMMENT '领用产线',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_created_at (created_at),
    INDEX idx_production_line (production_line)
) ENGINE=InnoDB COMMENT='出库流水表';

CREATE TABLE IF NOT EXISTS inventory_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quarter VARCHAR(20) NOT NULL COMMENT '盘点季度',
    total_count INT NOT NULL DEFAULT 0 COMMENT '盘点配件总数',
    match_count INT NOT NULL DEFAULT 0 COMMENT '账实相符数',
    diff_count INT NOT NULL DEFAULT 0 COMMENT '差异数',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_quarter (quarter)
) ENGINE=InnoDB COMMENT='盘点主表';

CREATE TABLE IF NOT EXISTS inventory_check_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_id BIGINT NOT NULL COMMENT '盘点主表ID',
    part_id BIGINT NOT NULL COMMENT '配件ID',
    part_name VARCHAR(100) COMMENT '配件名称(快照)',
    part_model VARCHAR(100) COMMENT '配件型号(快照)',
    shelf_position VARCHAR(50) COMMENT '货架位置(快照)',
    book_quantity INT NOT NULL COMMENT '账面数量',
    actual_quantity INT NOT NULL COMMENT '实物数量',
    difference INT NOT NULL COMMENT '差额(实物-账面)',
    INDEX idx_check_id (check_id),
    INDEX idx_part_id (part_id)
) ENGINE=InnoDB COMMENT='盘点明细表';

CREATE TABLE IF NOT EXISTS scrap_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '配件ID',
    part_name VARCHAR(100) COMMENT '配件名称(快照)',
    part_model VARCHAR(100) COMMENT '配件型号(快照)',
    quantity INT NOT NULL COMMENT '报废数量',
    reason VARCHAR(200) NOT NULL COMMENT '报废原因',
    remark VARCHAR(500) COMMENT '备注',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='报废记录表';
