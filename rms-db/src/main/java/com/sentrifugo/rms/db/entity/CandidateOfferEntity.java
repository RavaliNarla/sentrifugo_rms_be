package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.OfferStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_offers", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.candidate_offers SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateOfferEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false, unique = true)
    private UUID candidateId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "accept_before_date", nullable = false)
    private LocalDate acceptBeforeDate;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "offer_file_url")
    private String offerFileUrl;

    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OfferStatus status = OfferStatus.GENERATED;

    @Column(name = "accept_token", unique = true)
    private UUID acceptToken;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "decided_date")
    private LocalDateTime decidedDate;
}
