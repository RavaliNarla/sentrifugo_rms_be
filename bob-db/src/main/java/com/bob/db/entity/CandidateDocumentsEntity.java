package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.UUID;

@Data
@Entity
@Table(name = "candidate_documents" ,schema = "candidate")
@SQLDelete(sql = "UPDATE candidate.candidate_documents SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateDocumentsEntity  extends BaseEntity<UUID>{

//    @Id
//    @Column(name = "document_id")
//    private UUID documentId;

//    @Column(name = "candidate_id")
//    private UUID candidateId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "uploaded_date")
    private LocalDateTime uploadedDate;

    @Column(name = "comments")
    private String comments;

    @Column(name = "offer_status")
    private String offerStatus;

    @Column(name = "medical_centre_id")
    private UUID medicalCentreId;

}
