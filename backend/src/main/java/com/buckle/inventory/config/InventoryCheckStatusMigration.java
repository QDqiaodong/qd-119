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
public class InventoryCheckStatusMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryCheckStatusMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public InventoryCheckStatusMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureInventoryCheckStatusColumn();
        } catch (Exception e) {
            log.warn("InventoryCheckStatusMigration skipped due to: {}", e.getMessage());
        }
    }

    private void ensureInventoryCheckStatusColumn() {
        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory_check' AND COLUMN_NAME = 'status'");
            if (cols.isEmpty()) {
                jdbcTemplate.execute("""
                        ALTER TABLE inventory_check
                        ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-进行中 1-已完成' AFTER operator
                        """);
                log.info("[Migration] inventory_check.status column added.");
            }
        } catch (Exception e) {
            log.warn("[Migration] ensureInventoryCheckStatusColumn warning: {}", e.getMessage());
        }

        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory_check' AND INDEX_NAME = 'idx_status'",
                    Long.class);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("CREATE INDEX idx_status ON inventory_check(status)");
                log.info("[Migration] inventory_check.idx_status index created.");
            } catch (Exception ex) {
                log.warn("[Migration] index creation skipped: {}", ex.getMessage());
            }
        }
    }
}
