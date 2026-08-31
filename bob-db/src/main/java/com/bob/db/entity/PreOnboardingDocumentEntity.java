package com.bob.db.entity;

import com.bob.db.enums.PreOnboardingDocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_documents", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE candidate.pre_onboarding_documents SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingDocumentEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_documents";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "reference_table", length = 100)
    private String referenceTable;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private PreOnboardingDocumentType documentType;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "document_url", columnDefinition = "TEXT")
    private String documentUrl;


}