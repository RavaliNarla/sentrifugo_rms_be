package com.bob.db.entity;

import com.bob.db.enums.CandidateOfferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_offers", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE candidate.candidate_offers SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateOffersEntity extends BaseEntity<UUID> {

    @Column(name = "offer_release_date")
    private LocalDate offerReleaseDate;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "designation", length = 255)
    private UUID designation;

    @Column(name = "ctc")
    private BigDecimal ctc;

    @Column(name = "bonus")
    private BigDecimal bonus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private CandidateOfferStatus status = CandidateOfferStatus.OFFER_AWAITED;

    @ManyToOne
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false)
    private CandidateApplicationsEntity candidateApplication;

    @Column(name = "select_list")
    private String selectList;

    @Column(name = "wait_list")
    private String waitList;

    @Column(name = "accept_before_date")
    private LocalDate acceptBeforeDate;

    @Column(name = "template_id")
    private UUID templateId;

    @ManyToOne
    @JoinColumn(name = "position_id", referencedColumnName = "id")
    private JobPositionsEntity jobPosition;

    @OneToOne
    @JoinColumn(name = "candidate_id", referencedColumnName = "id")
    private CandidatesEntity candidate;

    @OneToOne
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    private InterviewCentresEntity interviewCenter;

    @OneToOne
    @JoinColumn(name= "interview_schedule_id", referencedColumnName = "id")
    private InterviewScheduleEntity interviewSchedule;

    @Column(name="offer_file_url")
    private String offerFileUrl;

    @Column(name="medical_center_id")
    private UUID medicalCenterId;

    @Column(name="candidate_comments")
    private String candidateComments;

    @Column(name="is_qualified")
    private boolean isQualified;

    @Column(name = "signatry_id")
    private UUID signatryId;

    @Column(name = "signatory_name", columnDefinition = "text")
    private String signatoryName;

    @Column(name = "signatory_designation", columnDefinition = "text")
    private String signatoryDesignation;

    @Column(name = "letter_number")
    private String letterNumber;
}
