package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "notifications", schema = "hr")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE hr.notifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class NotificationEntity extends BaseEntity<UUID> {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** REQUISITION_SUBMITTED | REQUISITION_APPROVED | REQUISITION_REJECTED | OFFER_SUBMITTED | OFFER_APPROVED | OFFER_REJECTED | INTERVIEW_SCHEDULED */
    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Null = unread. Set when the user opens the notifications panel. */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** For committee daily interview aggregation (interview date). */
    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "interview_count")
    @Builder.Default
    private Integer interviewCount = 0;

    /**
     * User who triggered the event (e.g. recruiter who submitted a req/offer or scheduled).
     * Unread badge excludes rows where sourceUserId equals the recipient.
     */
    @Column(name = "source_user_id")
    private UUID sourceUserId;
}
