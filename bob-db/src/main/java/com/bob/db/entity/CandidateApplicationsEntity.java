package com.bob.db.entity;

import com.bob.db.enums.ApplicationPaymentStatus;
import com.bob.db.enums.CandidateApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_applications" ,schema = "candidate")
@Data
@SQLDelete(sql = "UPDATE candidate.candidate_applications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true AND payment_status = 'SUCCESS'")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateApplicationsEntity  extends BaseEntity<UUID>{

    public static final String ENTITY_TYPE = "candidate_applications";

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status")
    @Builder.Default
    private CandidateApplicationStatus applicationStatus = CandidateApplicationStatus.APPLIED;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "ctc",precision = 15, scale = 2)
    private BigDecimal ctc;

    @Column(name = "designation")
    private String designation;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "application_no", length = 40, unique = true, insertable = false, updatable = false)
    private String applicationNo;

    @Column(name = "is_absent", nullable = false)
    @Builder.Default
    private Boolean isAbsent = false;

    @Column(name = "status_reason", columnDefinition = "text")
    private String statusReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", columnDefinition = "text")
    @Builder.Default
    private ApplicationPaymentStatus paymentStatus=ApplicationPaymentStatus.PENDING;

    @Column(name = "exam_centre_id")
    private UUID examCenterId;

    @Column(name = "stepper")
    private Integer stepper ;
}
