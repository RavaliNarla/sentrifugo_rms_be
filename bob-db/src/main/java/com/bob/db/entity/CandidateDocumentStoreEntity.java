package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "candidate_document_store", schema = "candidate")
@SQLDelete(sql = "UPDATE candidate.candidate_document_store SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateDocumentStoreEntity  extends BaseEntity<UUID>{

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "uploaded_date")
    @UpdateTimestamp
    private LocalDateTime uploadedDate;

    @Column(name = "isdigilocker")
    private boolean isDigilocker;

    @Column(name = "document_identifier")
    private UUID documentIdentifier;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "is_validation_pending")
    private Boolean isValidationPending;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_checks", columnDefinition = "jsonb")
    private List<String> pendingChecks;

}
