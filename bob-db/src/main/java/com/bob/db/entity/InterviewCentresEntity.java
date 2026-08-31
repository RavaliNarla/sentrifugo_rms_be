package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@Entity
@Table(name = "interview_centres", schema = "common")
@SQLDelete(sql = "UPDATE common.interview_centres SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewCentresEntity extends BaseEntity<UUID> {

    @Column(name = "interview_centre", length = 255, nullable = false)
    private String interviewCentre;

//    @Column(name = "organization_name")
//    private String organizationName;

    @Column(name = "organization_type")
    private String organizationType;

    @Column(name = "zone")
    private String zone;

    @Column(name = "zonal_state_id")
    private UUID zonalStateId;

    @Column(name = "alpha")
    private String alpha;

    @Column(name ="display_name")
    private String displayName;

}
