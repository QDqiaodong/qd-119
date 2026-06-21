package com.buckle.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PartSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PartSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public PartSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureAccessoryCategoryTable();
            ensurePartCategoryIdColumn();
            migrateOrphanPartsToDefaultCategory();
        } catch (Exception e) {
            log.warn("PartSchemaMigration skipped due to: {}", e.getMessage());
        }
    }

    private void ensureAccessoryCategoryTable() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB COMMENT='配件类别表'
                """);

        jdbcTemplate.execute("""
                INSERT INTO accessory_category (name, code, description, sort_order) VALUES
                ('卡扣', 'BUCKLE', '包装机卡扣类配件', 1),
                ('固定支架', 'BRACKET', '包装机固定支架类配件', 2),
                ('联接片', 'CONNECTOR', '包装机联接片类配件', 3),
                ('压板', 'PRESSURE_PLATE', '包装机压板类配件', 4),
                ('其他', 'OTHER', '其他包装机配件', 99)
                ON DUPLICATE KEY UPDATE name=VALUES(name)
                """);
        log.info("[Migration] accessory_category table ensured.");
    }

    private void ensurePartCategoryIdColumn() {
        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'part' AND COLUMN_NAME = 'category_id'");
            if (cols.isEmpty()) {
                jdbcTemplate.execute("""
                        ALTER TABLE part
                        ADD COLUMN category_id BIGINT NULL COMMENT '配件类别ID'
                        """);
                log.info("[Migration] part.category_id column added.");
            } else {
                log.info("[Migration] part.category_id column already exists, skipping.");
            }
        } catch (Exception e) {
            log.warn("[Migration] ensurePartCategoryIdColumn warning: {}", e.getMessage());
        }
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'part' AND INDEX_NAME = 'idx_category_id'",
                    Long.class);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("CREATE INDEX idx_category_id ON part(category_id)");
                log.info("[Migration] part.idx_category_id index created.");
            } catch (Exception ex) {
                log.warn("[Migration] index creation skipped: {}", ex.getMessage());
            }
        }
    }

    private void migrateOrphanPartsToDefaultCategory() {
        try {
            Long otherCategoryId = jdbcTemplate.queryForObject(
                    "SELECT id FROM accessory_category WHERE code = 'OTHER' LIMIT 1", Long.class);
            if (otherCategoryId == null) {
                log.warn("[Migration] OTHER category not found, skipping orphan migration.");
                return;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE part SET category_id = ? WHERE category_id IS NULL OR category_id = 0",
                    otherCategoryId);
            if (updated > 0) {
                log.info("[Migration] Migrated {} orphan parts to OTHER category (id={})", updated, otherCategoryId);
            } else {
                log.info("[Migration] No orphan parts found.");
            }
        } catch (Exception e) {
            log.warn("[Migration] migrateOrphanPartsToDefaultCategory warning: {}", e.getMessage());
        }
    }
}
