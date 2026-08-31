package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_requisition_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_requisition_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobRequisitionHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "action_type", length = 10)
    private String actionType;

    @Column(name = "requisition_title")
    private String requisitionTitle;

    @Column(name = "requisition_description", columnDefinition = "text")
    private String requisitionDescription;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "requisition_status", length = 50)
    private String requisitionStatus;

    @Column(name = "requisition_comments", columnDefinition = "text")
    private String requisitionComments;

    @Column(name = "indent_path", columnDefinition = "text")
    private String indentPath;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_date")
    private LocalDateTime changedDate;

    @Column(name = "cutoff_date")
    private LocalDate cutoffDate;
}
