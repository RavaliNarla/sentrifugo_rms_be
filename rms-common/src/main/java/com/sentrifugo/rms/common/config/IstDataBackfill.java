package com.sentrifugo.rms.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot data repairs: IST notification timestamps + ownership (created_by) backfills.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IstDataBackfill implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensurePatchTable();
        backfillNotificationTimestampsToIst();
        backfillRequisitionCreatedByFromSubmitHistory();
        backfillOfferCreatedByFromSubmitHistory();
        backfillInterviewScheduleCreatedBy();
    }

    private void ensurePatchTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS hr.schema_patches (
                      patch_id varchar(100) PRIMARY KEY,
                      applied_at timestamp NOT NULL
                    )
                    """);
        } catch (Exception e) {
            log.warn("Could not ensure hr.schema_patches: {}", e.getMessage());
        }
    }

    private boolean alreadyApplied(String patchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hr.schema_patches WHERE patch_id = ?",
                Integer.class,
                patchId);
        return count != null && count > 0;
    }

    private void markApplied(String patchId) {
        jdbcTemplate.update(
                "INSERT INTO hr.schema_patches (patch_id, applied_at) VALUES (?, NOW()) ON CONFLICT DO NOTHING",
                patchId);
    }

    /**
     * Older rows stored UTC wall-clock in LocalDateTime (Azure JVM default).
     * Shift once by +5h30m so they match Asia/Kolkata. New writes use IST after IstTimezoneBootstrap.
     */
    private void backfillNotificationTimestampsToIst() {
        final String patchId = "notifications_created_date_utc_to_ist_v1";
        try {
            if (alreadyApplied(patchId)) {
                return;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE hr.notifications
                    SET created_date = created_date + INTERVAL '5 hours 30 minutes',
                        modified_date = CASE
                            WHEN modified_date IS NOT NULL
                            THEN modified_date + INTERVAL '5 hours 30 minutes'
                            ELSE NULL END,
                        read_at = CASE
                            WHEN read_at IS NOT NULL
                            THEN read_at + INTERVAL '5 hours 30 minutes'
                            ELSE NULL END
                    WHERE created_date IS NOT NULL
                    """);
            markApplied(patchId);
            log.info("Backfilled {} notification timestamp(s) UTC→IST (+5h30m)", updated);
        } catch (Exception e) {
            log.warn("Could not backfill notification IST timestamps: {}", e.getMessage());
        }
    }

    private void backfillRequisitionCreatedByFromSubmitHistory() {
        final String patchId = "job_requisitions_created_by_from_l1_pending_v1";
        try {
            if (alreadyApplied(patchId)) {
                return;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE recruitment.job_requisitions r
                    SET created_by = h.approver_id
                    FROM (
                      SELECT DISTINCT ON (requisition_id) requisition_id, approver_id
                      FROM recruitment.requisition_approval_history
                      WHERE status = 'L1_PENDING' AND approver_id IS NOT NULL
                      ORDER BY requisition_id, COALESCE(occurred_at, created_date) DESC NULLS LAST
                    ) h
                    WHERE r.id = h.requisition_id
                      AND r.created_by IS NULL
                    """);
            markApplied(patchId);
            log.info("Backfilled created_by on {} requisition(s) from L1_PENDING history", updated);
        } catch (Exception e) {
            log.warn("Could not backfill requisition created_by: {}", e.getMessage());
        }
    }

    private void backfillOfferCreatedByFromSubmitHistory() {
        final String patchId = "candidate_offers_created_by_from_l1_pending_v1";
        try {
            if (alreadyApplied(patchId)) {
                return;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE recruitment.candidate_offers o
                    SET created_by = h.approver_id
                    FROM (
                      SELECT DISTINCT ON (offer_id) offer_id, approver_id
                      FROM recruitment.offer_approval_history
                      WHERE status = 'L1_PENDING' AND approver_id IS NOT NULL
                      ORDER BY offer_id, COALESCE(occurred_at, created_date) DESC NULLS LAST
                    ) h
                    WHERE o.id = h.offer_id
                      AND o.created_by IS NULL
                    """);
            markApplied(patchId);
            log.info("Backfilled created_by on {} offer(s) from L1_PENDING history", updated);
        } catch (Exception e) {
            log.warn("Could not backfill offer created_by: {}", e.getMessage());
        }
    }

    private void backfillInterviewScheduleCreatedBy() {
        final String patchId = "interview_schedule_created_by_from_modified_by_v1";
        try {
            if (alreadyApplied(patchId)) {
                return;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE recruitment.interview_schedule
                    SET created_by = modified_by
                    WHERE created_by IS NULL AND modified_by IS NOT NULL
                    """);
            markApplied(patchId);
            log.info("Backfilled created_by on {} interview schedule(s) from modified_by", updated);
        } catch (Exception e) {
            log.warn("Could not backfill interview_schedule created_by: {}", e.getMessage());
        }
    }
}
