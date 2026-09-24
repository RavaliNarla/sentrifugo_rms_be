package com.sentrifugo.rms.masterportal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.candidates ADD CONSTRAINT candidates_status_check CHECK (
                      status::text = ANY (ARRAY[
                        'ADDED','SHORTLISTED','REJECTED','ON_HOLD','INVITE_SENT','SCHEDULED','DECLINED',
                        'QUALIFIED','DISQUALIFIED','COMPENSATION_PENDING','MOVED_TO_OFFER'
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
    }
}
