package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_compensation", schema = "candidate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.application_compensation SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ApplicationCompensationEntity extends BaseEntity<UUID> implements Serializable {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "current_ctc", precision = 19, scale = 2)
    private BigDecimal currentCtc;

    @Column(name = "expected_ctc", precision = 19, scale = 2)
    private BigDecimal expectedCtc;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

}
