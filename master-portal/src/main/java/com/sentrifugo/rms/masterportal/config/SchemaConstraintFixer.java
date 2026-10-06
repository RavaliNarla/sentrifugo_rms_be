package com.sentrifugo.rms.masterportal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.sentrifugo.rms.common.util.MasterCodeUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hibernate ddl-auto=update does not revise Postgres CHECK constraints.
 * Keep candidates_status_check aligned with CandidateStatus (incl. REJECTED).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SchemaConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE recruitment.candidates DROP CONSTRAINT IF EXISTS candidates_status_check");
            int updated = jdbcTemplate.update(
                    "UPDATE recruitment.candidates SET status = 'REJECTED' WHERE status = 'NOT_SHORTLISTED'");
            if (updated > 0) {
                log.info("Backfilled {} candidate(s) from NOT_SHORTLISTED → REJECTED", updated);
            }
            // Bulk-imported candidates without a resume: DRAFT was renamed to RESUME_PENDING.
            int resumePending = jdbcTemplate.update(
                    "UPDATE recruitment.candidates SET status = 'RESUME_PENDING' WHERE status = 'DRAFT'");
            if (resumePending > 0) {
                log.info("Backfilled {} candidate(s) from DRAFT → RESUME_PENDING", resumePending);
            }
            // 'DRAFT' stays allowed only while teammates' portals on the shared DB still run pre-rename code.
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.candidates ADD CONSTRAINT candidates_status_check CHECK (
                      status::text = ANY (ARRAY[
                        'RESUME_PENDING','DRAFT','ADDED','SHORTLISTED','REJECTED','ON_HOLD','INVITE_SENT','SCHEDULED','DECLINED',
                        'QUALIFIED','DISQUALIFIED','COMPENSATION_PENDING','COMPENSATION_SUBMITTED','MOVED_TO_OFFER'
                      ]::text[])
                    )
                    """);
            log.info("Ensured recruitment.candidates_status_check includes INVITE_SENT and DECLINED");
        } catch (Exception e) {
            log.warn("Could not refresh candidates_status_check: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE recruitment.candidate_offers DROP CONSTRAINT IF EXISTS candidate_offers_status_check");
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.candidate_offers ADD CONSTRAINT candidate_offers_status_check CHECK (
                      status::text = ANY (ARRAY[
                        'GENERATED','L1_PENDING','L2_PENDING','L1_REJECTED','L2_REJECTED',
                        'SENT','ACCEPTED','REJECTED','EXPIRED'
                      ]::text[])
                    )
                    """);
            log.info("Ensured recruitment.candidate_offers_status_check includes approval + decision statuses");
        } catch (Exception e) {
            log.warn("Could not refresh candidate_offers_status_check: {}", e.getMessage());
        }

        ensureMasterCodes();
    }

    /** Add unique 3-char codes on departments/locations and backfill existing rows. */
    private void ensureMasterCodes() {
        try {
            jdbcTemplate.execute("ALTER TABLE common.departments ADD COLUMN IF NOT EXISTS code varchar(3)");
            jdbcTemplate.execute("ALTER TABLE common.locations ADD COLUMN IF NOT EXISTS code varchar(3)");
        } catch (Exception e) {
            log.warn("Could not add master code columns: {}", e.getMessage());
            return;
        }

        try {
            backfillCodes("common.departments");
            backfillCodes("common.locations");
        } catch (Exception e) {
            log.warn("Could not backfill master codes: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uq_departments_code
                    ON common.departments (upper(code)) WHERE code IS NOT NULL
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_code
                    ON common.locations (upper(code)) WHERE code IS NOT NULL
                    """);
        } catch (Exception e) {
            log.warn("Could not ensure master code unique indexes: {}", e.getMessage());
        }
    }

    private void backfillCodes(String table) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, code FROM " + table + " ORDER BY created_date NULLS LAST, name");
        Set<String> used = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object existing = row.get("code");
            if (existing != null && !existing.toString().isBlank()) {
                used.add(existing.toString().trim().toUpperCase(Locale.ROOT));
            }
        }
        int updated = 0;
        for (Map<String, Object> row : rows) {
            Object existing = row.get("code");
            if (existing != null && !existing.toString().isBlank()) {
                continue;
            }
            String name = row.get("name") != null ? row.get("name").toString() : "";
            String code = MasterCodeUtil.suggestFromName(name, used);
            used.add(code);
            jdbcTemplate.update("UPDATE " + table + " SET code = ? WHERE id = ?", code, row.get("id"));
            updated++;
        }
        if (updated > 0) {
            log.info("Backfilled {} code(s) on {}", updated, table);
        }
    }
}
