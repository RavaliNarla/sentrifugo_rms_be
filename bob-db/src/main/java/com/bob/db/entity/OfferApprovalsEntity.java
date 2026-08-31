package com.bob.db.entity;

import com.bob.db.enums.OfferApprovalStatus;
import com.bob.db.enums.RequisitionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "offer_approvals", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE candidate.offer_approvals SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class OfferApprovalsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "offer_approvals";


    @Column(name = "offer_id")
    private UUID offerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false)
    private CandidateApplicationsEntity candidateApplication;

    // Used strictly for insertion and updates
    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    // Read-only mapping used strictly for fetching display data
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "candidate_id", insertable = false, updatable = false)
    private CandidateProfileEntity candidateProfile;

    @Column(name = "designation_id")
    private UUID designationId;

    @Column(name = "ctc", precision = 12, scale = 2)
    private BigDecimal ctc;

    @Column(name = "bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "offer_release_date")
    private LocalDate offerReleaseDate;

    @Column(name = "accept_before_date")
    private LocalDate acceptBeforeDate;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "offer_file_url", columnDefinition = "text")
    private String offerFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    @Builder.Default
    private OfferApprovalStatus approvalStatus=OfferApprovalStatus.L1_PENDING;


    @Column(name = "latest_comments", columnDefinition = "text")
    private String latestComments;
}