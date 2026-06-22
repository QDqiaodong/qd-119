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
public class PackagingMachineMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PackagingMachineMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public PackagingMachineMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensurePackagingMachineTable();
            ensureOutboundRecordMachineColumns();
            initPackagingMachines();
        } catch (Exception e) {
            log.warn("PackagingMachineMigration skipped due to: {}", e.getMessage());
        }
    }

    private void ensurePackagingMachineTable() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB COMMENT='包装机机台表'
                """);
        log.info("[Migration] packaging_machine table ensured.");
    }

    private void ensureOutboundRecordMachineColumns() {
        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'outbound_record' AND COLUMN_NAME = 'machine_id'");
            if (cols.isEmpty()) {
                jdbcTemplate.execute("""
                        ALTER TABLE outbound_record
                        ADD COLUMN machine_id BIGINT NULL COMMENT '包装机机台ID' AFTER production_line
                        """);
                log.info("[Migration] outbound_record.machine_id column added.");
            }
        } catch (Exception e) {
            log.warn("[Migration] ensureOutboundRecordMachineIdColumn warning: {}", e.getMessage());
        }

        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'outbound_record' AND COLUMN_NAME = 'machine_code'");
            if (cols.isEmpty()) {
                jdbcTemplate.execute("""
                        ALTER TABLE outbound_record
                        ADD COLUMN machine_code VARCHAR(50) NULL COMMENT '机台编号(快照)' AFTER machine_id
                        """);
                log.info("[Migration] outbound_record.machine_code column added.");
            }
        } catch (Exception e) {
            log.warn("[Migration] ensureOutboundRecordMachineCodeColumn warning: {}", e.getMessage());
        }

        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'outbound_record' AND INDEX_NAME = 'idx_machine_id'",
                    Long.class);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("CREATE INDEX idx_machine_id ON outbound_record(machine_id)");
                log.info("[Migration] outbound_record.idx_machine_id index created.");
            } catch (Exception ex) {
                log.warn("[Migration] index creation skipped: {}", ex.getMessage());
            }
        }
    }

    private void initPackagingMachines() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM packaging_machine", Integer.class);
        if (count != null && count > 0) {
            log.info("[Migration] packaging_machine already has data, skipping init.");
            return;
        }

        jdbcTemplate.execute("""
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
                """);
        log.info("[Migration] packaging_machine initial data inserted.");
    }
}
