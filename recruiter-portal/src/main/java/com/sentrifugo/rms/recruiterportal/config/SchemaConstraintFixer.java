package com.sentrifugo.rms.recruiterportal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keep Postgres CHECK constraints in sync with enums; backfill renamed statuses. */
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
            // Backfill old shortlist-reject label before re-adding the CHECK.
            int updated = jdbcTemplate.update(
                    "UPDATE recruitment.candidates SET status = 'REJECTED' WHERE status = 'NOT_SHORTLISTED'");
            if (updated > 0) {
                log.info("Backfilled {} candidate(s) from NOT_SHORTLISTED → REJECTED", updated);
            }
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.candidates ADD CONSTRAINT candidates_status_check CHECK (
                      status::text = ANY (ARRAY[
                        'DRAFT','ADDED','SHORTLISTED','REJECTED','ON_HOLD','INVITE_SENT','SCHEDULED','DECLINED',
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

        // Interview invite Accept/Decline token columns (Hibernate may also add; IF NOT EXISTS is safe).
        try {
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ADD COLUMN IF NOT EXISTS accept_token uuid");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ADD COLUMN IF NOT EXISTS superseded_tokens varchar(4000)");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ADD COLUMN IF NOT EXISTS invite_sent_at timestamp");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_schedule ADD COLUMN IF NOT EXISTS invite_responded_at timestamp");
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_interview_schedule_accept_token
                    ON recruitment.interview_schedule (accept_token)
                    WHERE accept_token IS NOT NULL
                    """);
            log.info("Ensured recruitment.interview_schedule invite token columns");
        } catch (Exception e) {
            log.warn("Could not ensure interview_schedule invite columns: {}", e.getMessage());
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

        try {
            jdbcTemplate.execute("""
                    ALTER TABLE recruitment.interview_schedule
                    ADD COLUMN IF NOT EXISTS round_name varchar(120)
                    """);
            log.info("Ensured recruitment.interview_schedule.round_name exists");
        } catch (Exception e) {
            log.warn("Could not ensure interview_schedule.round_name: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS recruitment.interview_round_meta (
                      id uuid PRIMARY KEY,
                      candidate_id uuid NOT NULL,
                      round integer NOT NULL,
                      round_name varchar(120),
                      panel_id uuid,
                      interview_date date,
                      start_time time,
                      end_time time,
                      duration_minutes integer,
                      created_by uuid,
                      created_date timestamp,
                      modified_by uuid,
                      modified_date timestamp,
                      is_active boolean DEFAULT true
                    )
                    """);
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_round_meta ADD COLUMN IF NOT EXISTS panel_id uuid");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_round_meta ADD COLUMN IF NOT EXISTS interview_date date");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_round_meta ADD COLUMN IF NOT EXISTS start_time time");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_round_meta ADD COLUMN IF NOT EXISTS end_time time");
            jdbcTemplate.execute("ALTER TABLE recruitment.interview_round_meta ADD COLUMN IF NOT EXISTS duration_minutes integer");
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_interview_round_meta_candidate_round
                    ON recruitment.interview_round_meta (candidate_id, round)
                    WHERE is_active = true
                    """);
            // Seed current live schedules so Panel Management history is not empty after deploy.
            jdbcTemplate.execute("""
                    INSERT INTO recruitment.interview_round_meta (
                      id, candidate_id, round, round_name, panel_id, interview_date,
                      start_time, end_time, duration_minutes, is_active, created_date
                    )
                    SELECT gen_random_uuid(), s.candidate_id, COALESCE(s.round, 1), s.round_name, s.panel_id,
                           s.interview_date, s.start_time, s.end_time, s.duration_minutes, true, NOW()
                    FROM recruitment.interview_schedule s
                    WHERE COALESCE(s.is_active, true) = true
                      AND NOT EXISTS (
                        SELECT 1 FROM recruitment.interview_round_meta m
                        WHERE m.candidate_id = s.candidate_id
                          AND m.round = COALESCE(s.round, 1)
                          AND COALESCE(m.is_active, true) = true
                      )
                    """);
            log.info("Ensured recruitment.interview_round_meta exists (with schedule snapshot columns)");
        } catch (Exception e) {
            log.warn("Could not ensure interview_round_meta: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE hr.notifications ADD COLUMN IF NOT EXISTS source_user_id uuid");
            log.info("Ensured hr.notifications.source_user_id exists");
        } catch (Exception e) {
            log.warn("Could not ensure notifications.source_user_id: {}", e.getMessage());
        }
    }
}
