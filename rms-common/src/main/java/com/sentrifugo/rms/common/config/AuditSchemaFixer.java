package com.sentrifugo.rms.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures {@code hr.audit_events} matches the field-level schema
 * (correlation_id, event_id, field_name, before_value, after_value).
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AuditSchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Prefer a clean recreate when the previous JSON-shaped table is present and empty / unused.
            migrateFromJsonShapeIfNeeded();

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS hr.audit_events (
                      id uuid PRIMARY KEY,
                      correlation_id uuid NOT NULL,
                      event_id uuid NOT NULL,
                      occurred_at timestamptz NOT NULL,
                      actor_id uuid,
                      actor_name varchar(255),
                      actor_email varchar(255),
                      action varchar(32) NOT NULL,
                      entity_type varchar(64) NOT NULL,
                      entity_id uuid,
                      field_name varchar(128) NOT NULL,
                      before_value text,
                      after_value text,
                      portal varchar(64),
                      summary text
                    )
                    """);

            // In case an older empty table existed without new columns.
            jdbcTemplate.execute("ALTER TABLE hr.audit_events ADD COLUMN IF NOT EXISTS correlation_id uuid");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events ADD COLUMN IF NOT EXISTS event_id uuid");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events ADD COLUMN IF NOT EXISTS field_name varchar(128)");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events ADD COLUMN IF NOT EXISTS before_value text");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events ADD COLUMN IF NOT EXISTS after_value text");

            jdbcTemplate.execute("ALTER TABLE hr.audit_events DROP COLUMN IF EXISTS before_json");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events DROP COLUMN IF EXISTS after_json");
            jdbcTemplate.execute("ALTER TABLE hr.audit_events DROP COLUMN IF EXISTS changes_json");

            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_at
                      ON hr.audit_events (occurred_at DESC)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_audit_events_entity
                      ON hr.audit_events (entity_type, entity_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_audit_events_actor
                      ON hr.audit_events (actor_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_audit_events_correlation
                      ON hr.audit_events (correlation_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_audit_events_event
                      ON hr.audit_events (event_id)
                    """);
            log.info("Ensured hr.audit_events field-level table and indexes");
        } catch (Exception e) {
            log.warn("Could not ensure hr.audit_events: {}", e.getMessage());
        }
    }

    private void migrateFromJsonShapeIfNeeded() {
        try {
            Integer hasJson = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'hr' AND table_name = 'audit_events' AND column_name = 'changes_json'
                    """, Integer.class);
            if (hasJson == null || hasJson == 0) {
                return;
            }
            Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hr.audit_events", Integer.class);
            // Drop and recreate when still on the JSON shape (dev/preview data only expected).
            log.info("Replacing hr.audit_events JSON shape with field-level shape (existing_rows={})", rows);
            jdbcTemplate.execute("DROP TABLE IF EXISTS hr.audit_events");
        } catch (Exception e) {
            log.warn("audit_events JSON migration check skipped: {}", e.getMessage());
        }
    }
}
