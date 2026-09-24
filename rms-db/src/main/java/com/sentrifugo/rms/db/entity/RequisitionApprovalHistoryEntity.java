package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/** One row per approval action on a requisition (SCL_53 history modal). */
@Entity
@Table(name = "requisition_approval_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.requisition_approval_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RequisitionApprovalHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Column(name = "approver_name")
    private String approverName;

    /** Status after this action, e.g. L2_PENDING, L1_REJECTED, APPROVED. */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // Explicit UTC instant for this action - unlike the inherited createdDate (a timezone-less
    // LocalDateTime captured in the server JVM's default zone), this always serializes with an
    // offset so the frontend can correctly convert it to the viewer's local time.
    @Column(name = "occurred_at")
    private Instant occurredAt;
}
