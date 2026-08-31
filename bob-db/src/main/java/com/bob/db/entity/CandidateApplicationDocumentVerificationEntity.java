package com.bob.db.entity;

import com.bob.db.enums.DocumentScreeningStatus;
import com.bob.db.enums.DocumentZonalVerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "candidate_application_document_verification", schema = "candidate")
@SQLDelete(sql = "UPDATE candidate.candidate_application_document_verification SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateApplicationDocumentVerificationEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_document_id")
    private UUID candidateDocumentId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_screening_status", length = 50)
    @Builder.Default
    private DocumentScreeningStatus docScreeningStatus = DocumentScreeningStatus.PENDING;

    @Column(name = "doc_screening_comments", columnDefinition = "text")
    private String docScreeningComments;

    @Column(name = "last_screened_by_user_id")
    private UUID lastScreenedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "zonal_hr_doc_status", length = 50)
    @Builder.Default
    private DocumentZonalVerificationStatus zonalHrDocStatus = DocumentZonalVerificationStatus.PENDING;

    @Column(name = "zonal_hr_doc_comments", columnDefinition = "text")
    private String zonalHrDocComments;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "is_validation_pending")
    private Boolean isValidationPending;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_checks", columnDefinition = "jsonb")
    private List<String> pendingChecks;

    @Column(name = "isdigilocker")
    private Boolean isDigilocker;
}

