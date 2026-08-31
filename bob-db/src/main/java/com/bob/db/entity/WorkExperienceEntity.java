package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_experience", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.work_experience SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class WorkExperienceEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "organization_name", length = 250, nullable = false)
    private String organizationName;

    @Column(name = "role", length = 200)
    private String role;

    @Column(name = "post_held", length = 255)
    private String postHeld;

    @Column(name = "is_presently_working")
    @Builder.Default
    private Boolean isPresentlyWorking = false;

    @Column(name = "work_description", columnDefinition = "text")
    private String workDescription;

    @Column(name = "months_of_exp")
    private Short monthsOfExp;

    @Column(name = "current_ctc", precision = 15, scale = 2, nullable = true)
    private BigDecimal currentCtc;
}
