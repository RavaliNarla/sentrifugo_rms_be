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

import java.util.UUID;

/** One row per approval action on an offer letter (Offer Approvals history modal). */
@Entity
@Table(name = "offer_approval_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.offer_approval_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class OfferApprovalHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Column(name = "approver_name")
    private String approverName;

    /** Status after this action, e.g. L1_PENDING, L2_PENDING, L1_REJECTED, L2_REJECTED, SENT. */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
