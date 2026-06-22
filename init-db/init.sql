CREATE DATABASE IF NOT EXISTS buckle_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE buckle_inventory;

CREATE TABLE IF NOT EXISTS accessory_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '类别名称',
    code VARCHAR(30) NOT NULL COMMENT '类别编码',
    description VARCHAR(200) COMMENT '类别描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_name (name)
) ENGINE=InnoDB COMMENT='配件类别表';

INSERT INTO accessory_category (name, code, description, sort_order) VALUES
('卡扣', 'BUCKLE', '包装机卡扣类配件', 1),
('固定支架', 'BRACKET', '包装机固定支架类配件', 2),
('联接片', 'CONNECTOR', '包装机联接片类配件', 3),
('压板', 'PRESSURE_PLATE', '包装机压板类配件', 4),
('其他', 'OTHER', '其他包装机配件', 99)
ON DUPLICATE KEY UPDATE name=VALUES(name);

CREATE TABLE IF NOT EXISTS part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL COMMENT '配件类别ID',
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
    INDEX idx_deleted (deleted),
    INDEX idx_category_id (category_id)
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
    machine_id BIGINT NULL COMMENT '包装机机台ID',
    machine_code VARCHAR(50) NULL COMMENT '机台编号(快照)',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_created_at (created_at),
    INDEX idx_production_line (production_line),
    INDEX idx_machine_id (machine_id)
) ENGINE=InnoDB COMMENT='出库流水表';

CREATE TABLE IF NOT EXISTS packaging_machine (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_code VARCHAR(50) NOT NULL COMMENT '机台编号',
    machine_name VARCHAR(100) COMMENT '机台名称',
    production_line VARCHAR(50) NOT NULL COMMENT '所属产线',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_machine_code (machine_code),
    INDEX idx_production_line (production_line),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='包装机机台表';

INSERT INTO packaging_machine (machine_code, machine_name, production_line, sort_order) VALUES
('BZ-A01', '包装机A01', '产线A', 1),
('BZ-A02', '包装机A02', '产线A', 2),
('BZ-A03', '包装机A03', '产线A', 3),
('BZ-B01', '包装机B01', '产线B', 1),
('BZ-B02', '包装机B02', '产线B', 2),
('BZ-B03', '包装机B03', '产线B', 3),
('BZ-C01', '包装机C01', '产线C', 1),
('BZ-C02', '包装机C02', '产线C', 2),
('BZ-D01', '包装机D01', '产线D', 1),
('BZ-D02', '包装机D02', '产线D', 2)
ON DUPLICATE KEY UPDATE machine_name=VALUES(machine_name), production_line=VALUES(production_line);

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

CREATE TABLE IF NOT EXISTS scrap_reason_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL COMMENT '原因编码',
    name VARCHAR(50) NOT NULL COMMENT '原因名称',
    level VARCHAR(20) NOT NULL COMMENT '报废等级',
    description VARCHAR(200) COMMENT '原因描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0-否 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_level (level),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB COMMENT='报废原因字典表';

INSERT INTO scrap_reason_dict (code, name, level, description, sort_order) VALUES
('SLIGHT_DEFORMATION', '轻微变形', '一级', '配件轻微变形，不影响主要功能', 1),
('BUCKLE_BROKEN', '卡扣断齿', '二级', '包装机卡扣断齿，无法正常使用', 2),
('BRACKET_CRACKED', '支架开裂', '二级', '固定支架开裂，存在安全隐患', 3),
('RUST_JAMMED', '锈蚀卡死', '三级', '配件严重锈蚀卡死，完全无法使用', 4),
('OTHER', '其他', '三级', '其他报废原因', 99)
ON DUPLICATE KEY UPDATE name=VALUES(name), level=VALUES(level), description=VALUES(description), sort_order=VALUES(sort_order);
