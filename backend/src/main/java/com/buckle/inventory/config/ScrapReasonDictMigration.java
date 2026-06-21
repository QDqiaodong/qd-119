package com.buckle.inventory.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScrapReasonDictMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ScrapReasonDictMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB COMMENT='报废原因字典表'
                """);

        jdbcTemplate.execute("""
                INSERT INTO scrap_reason_dict (code, name, level, description, sort_order) VALUES
                ('SLIGHT_DEFORMATION', '轻微变形', '一级', '配件轻微变形，不影响主要功能', 1),
                ('BUCKLE_BROKEN', '卡扣断齿', '二级', '包装机卡扣断齿，无法正常使用', 2),
                ('BRACKET_CRACKED', '支架开裂', '二级', '固定支架开裂，存在安全隐患', 3),
                ('RUST_JAMMED', '锈蚀卡死', '三级', '配件严重锈蚀卡死，完全无法使用', 4),
                ('OTHER', '其他', '三级', '其他报废原因', 99)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    level = VALUES(level),
                    description = VALUES(description),
                    sort_order = VALUES(sort_order),
                    enabled = VALUES(enabled)
                """);
    }
}
