package com.mcalvaro.mscatering.infrastructure.support;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Removes committed data from the shared in-memory test database.
 * <p>
 * Integration tests that opt out of the rollback-based transaction must call {@link #clean()}
 * so they do not leak rows into other test classes running in the same JVM.
 */
@Component
public class DatabaseCleaner {

    private static final List<String> PRESERVED_TABLES = List.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            for (String table : tables) {
                if (!PRESERVED_TABLES.contains(table.toUpperCase())) {
                    jdbcTemplate.execute("TRUNCATE TABLE \"" + table + "\"");
                }
            }
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
