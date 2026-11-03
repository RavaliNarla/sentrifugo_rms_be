package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.AttendanceEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Standalone attendance entry log (client integration). Not a BaseEntity — no RMS audit trail.
 */
@Entity
@Table(name = "attendance_entries", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntryEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "emp_code", nullable = false, length = 32)
    private String empCode;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 8)
    private AttendanceEntryType entryType;

    /** When recruiter-portal received and stored this entry. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
