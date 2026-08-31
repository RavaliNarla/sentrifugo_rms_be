package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "candidate_concessions", schema = "candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_concessions SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateConcessionsEntity extends BaseEntity<UUID>{

    @Column(name="application_id")
    private UUID applicationId;

    @Column(name = "exam_concession")
    @Builder.Default
    private Boolean examConcession = false;

    @Column(name = "age_concession")
    @Builder.Default
    private Boolean ageConcession = false;

    @Column(name = "interview_concession")
    @Builder.Default
    private Boolean interviewConcession = false;
}
