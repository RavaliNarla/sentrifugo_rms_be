package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "candidate_certifications", schema = "candidate")
@SQLDelete(sql = "UPDATE candidate.candidate_certifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateCertificationsEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "certification_name", length = 255, nullable = false)
    private String certificationName;

    @Column(name = "issued_by", length = 255, nullable = false)
    private String issuedBy;

    @Column(name = "certification_date")
    private LocalDate certificationDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "certification_id")
    private UUID certificationId;

}

