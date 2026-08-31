package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "job_selection_process", schema = "recruitment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_selection_process SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobSelectionProcessEntity  extends BaseEntity<UUID>{

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "selection_procedure", columnDefinition = "text")
    private String selectionProcedure;
}