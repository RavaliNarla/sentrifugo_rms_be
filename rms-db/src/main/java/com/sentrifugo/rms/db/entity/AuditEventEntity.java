package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only field-level audit trail (one row per changed field).
 * Does <strong>not</strong> extend {@link BaseEntity} so audit writes are not audited.
 *
 * <p>{@code correlation_id} = whole HTTP request / job.
 * {@code event_id} = one entity mutation (all field rows from that save share it).
 */
@Entity
@Table(name = "audit_events", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Groups every audit row produced by one incoming request / background job. */
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    /** Groups every field row from one entity CREATE/UPDATE/DELETE. */
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    /** CREATE | UPDATE | DELETE | STATUS_CHANGE | DEACTIVATE | REACTIVATE */
    @Column(name = "action", nullable = false, length = 32)
    private String action;

    /** e.g. User, JobRequisition, Candidate */
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    /** Changed property name; {@code *} when there is no field-level detail. */
    @Column(name = "field_name", nullable = false, length = 128)
    private String fieldName;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "portal", length = 64)
    private String portal;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
}
