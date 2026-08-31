package com.bob.db.entity;

import com.bob.db.enums.RequisitionStatus;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_requisitions",schema = "recruitment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_requisitions SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobRequisitionsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "job_requisitions";

    @Column(name = "requisition_title")
    private String requisitionTitle;

    @Column(name = "requisition_description", columnDefinition = "TEXT")
    private String requisitionDescription;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "requisition_status", length = 50)
    @Builder.Default
    private RequisitionStatus requisitionStatus = RequisitionStatus.NEW;

    @Column(name = "requisition_comments", columnDefinition = "TEXT")
    private String requisitionComments;

    @Column(name = "requisition_code", insertable = false, updatable = false ,unique = true)
    private String requisitionCode;

    @Column(name = "is_in_edit_mode")
    @Builder.Default
    private Boolean isInEditMode = false;

    @Column(name = "parent_requisition_id")
    private UUID parentRequisitionId;

    @Column(name = "is_reinitialized")
    @Builder.Default
    private Boolean isReinitialized = false;

    @Column(name = "cutoff_date")
    private LocalDate cutoffDate;

    @Column(name = "is_hiring_completed")
    @Builder.Default
    private Boolean isHiringCompleted = false;

}
