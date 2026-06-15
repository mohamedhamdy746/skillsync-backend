package com.pentastack.skillsync.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

@Component
public class DataSourceInspector {
    private static final Logger log = LoggerFactory.getLogger(DataSourceInspector.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DataSourceInspector(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("Running DataSourceInspector checks...");
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            log.info("DataSource URL: {}", meta.getURL());
            log.info("Database product name: {}", meta.getDatabaseProductName());
            log.info("Database product version: {}", meta.getDatabaseProductVersion());
            log.info("Driver name: {}", meta.getDriverName());
            log.info("Driver version: {}", meta.getDriverVersion());
            log.info("Auto-commit: {}", conn.getAutoCommit());
            log.info("Transaction isolation: {}", conn.getTransactionIsolation());
        } catch (Exception e) {
            log.warn("Unable to inspect DataSource metadata: {}", e.getMessage(), e);
        }

        // Query information_schema for tables in public schema
        try {
            List<String> tables = jdbcTemplate.queryForList(
                    "select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE'",
                    String.class);

            log.info("Found {} tables in public schema: {}", tables.size(), tables);

            // Check for expected tables
            String[] expected = new String[]{"users", "stack", "mentor_profile", "student_profile", "review_sessions", "session_audit_logs"};
            for (String t : expected) {
                boolean present = tables.stream().anyMatch(x -> x.equalsIgnoreCase(t));
                log.info("Table '{}' present: {}", t, present);
            }
        } catch (Exception e) {
            log.warn("Unable to query information_schema: {}", e.getMessage());
        }
    }
}

