package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "job_position_exclusions_edit_requests", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_position_exclusions_edit_requests SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionExclusionsEditRequestEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_edit_position_id", nullable = false)
    private JobPositionEditRequestEntity jobEditPosition;

    @Column(name = "exclusion_id", nullable = false)
    private UUID exclusionId;

    @Column(name = "is_excluded", nullable = false)
    private Boolean isExcluded = false;
}
