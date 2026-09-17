package com.sentrifugo.rms.recruiterportal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** SCL_25: keep candidates_status_check in sync with CandidateStatus enum. */
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
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.candidates ADD CONSTRAINT candidates_status_check CHECK (
                      status::text = ANY (ARRAY[
                        'ADDED','SHORTLISTED','NOT_SHORTLISTED','ON_HOLD','SCHEDULED',
                        'QUALIFIED','DISQUALIFIED','COMPENSATION_PENDING','MOVED_TO_OFFER'
                      ]::text[])
                    )
                    """);
            log.info("Ensured recruitment.candidates_status_check includes ON_HOLD and NOT_SHORTLISTED");
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

        // Interview rounds: add column if missing, backfill 1, then enforce NOT NULL.
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.interview_schedule
                    ADD COLUMN IF NOT EXISTS round integer
                    """);
            jdbcTemplate.execute("UPDATE recruitment.interview_schedule SET round = 1 WHERE round IS NULL");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ALTER COLUMN round SET DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ALTER COLUMN round SET NOT NULL");
            log.info("Ensured recruitment.interview_schedule.round exists and defaults to 1");
        } catch (Exception e) {
            log.warn("Could not ensure interview_schedule.round: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.panel_member_scores
                    ADD COLUMN IF NOT EXISTS round integer
                    """);
            jdbcTemplate.execute("UPDATE recruitment.panel_member_scores SET round = 1 WHERE round IS NULL");
            jdbcTemplate.execute("ALTER TABLE recruitment.panel_member_scores ALTER COLUMN round SET DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE recruitment.panel_member_scores ALTER COLUMN round SET NOT NULL");
            log.info("Ensured recruitment.panel_member_scores.round exists and defaults to 1");
        } catch (Exception e) {
            log.warn("Could not ensure panel_member_scores.round: {}", e.getMessage());
        }
    }
}
