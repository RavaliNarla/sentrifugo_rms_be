package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.CandidateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "candidates", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.candidates SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateEntity extends BaseEntity<UUID> {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "id_proof_url")
    private String idProofUrl;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CandidateStatus status = CandidateStatus.ADDED;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    /** Legacy single-figure salary - superseded by the CTC breakdown below, kept for backward compatibility. */
    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    // --- Compensation Management (Section 15 of the requirements doc) ---
    @Column(name = "current_ctc", precision = 12, scale = 2)
    private BigDecimal currentCtc;

    @Column(name = "expected_ctc", precision = 12, scale = 2)
    private BigDecimal expectedCtc;

    @Column(name = "fixed_pay", precision = 12, scale = 2)
    private BigDecimal fixedPay;

    @Column(name = "variable_pay", precision = 12, scale = 2)
    private BigDecimal variablePay;

    @Column(name = "bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "compensation_comments", columnDefinition = "TEXT")
    private String compensationComments;

    /** The final negotiated CTC - this is what flows into the offer letter. */
    @Column(name = "agreed_ctc", precision = 12, scale = 2)
    private BigDecimal agreedCtc;
}
