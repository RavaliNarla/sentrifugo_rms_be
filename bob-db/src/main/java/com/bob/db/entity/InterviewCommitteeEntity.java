package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@Entity
@Table(name = "interview_committee" ,schema = "common")
@SQLDelete(sql = "UPDATE common.interview_panels SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewCommitteeEntity  extends BaseEntity<UUID>{
    @Column(name = "committee_name", nullable = false)
    private String committeeName;

    @Column(name = "committee_desc", columnDefinition = "text")
    private String committeeDesc;
}
