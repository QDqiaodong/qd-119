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
public class ScrapRecordConfirmedMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScrapRecordConfirmedMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public ScrapRecordConfirmedMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureScrapRecordConfirmedColumn();
        } catch (Exception e) {
            log.warn("ScrapRecordConfirmedMigration skipped due to: {}", e.getMessage());
        }
    }

    private void ensureScrapRecordConfirmedColumn() {
        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scrap_record' AND COLUMN_NAME = 'confirmed'");
            if (cols.isEmpty()) {
                jdbcTemplate.execute("""
                        ALTER TABLE scrap_record
                        ADD COLUMN confirmed TINYINT NOT NULL DEFAULT 1 COMMENT '是否确认 0-未确认 1-已确认' AFTER operator
                        """);
                log.info("[Migration] scrap_record.confirmed column added with default value 1.");
            }
        } catch (Exception e) {
            log.warn("[Migration] ensureScrapRecordConfirmedColumn warning: {}", e.getMessage());
        }

        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scrap_record' AND INDEX_NAME = 'idx_confirmed'",
                    Long.class);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("CREATE INDEX idx_confirmed ON scrap_record(confirmed)");
                log.info("[Migration] scrap_record.idx_confirmed index created.");
            } catch (Exception ex) {
                log.warn("[Migration] index creation skipped: {}", ex.getMessage());
            }
        }
    }
}
