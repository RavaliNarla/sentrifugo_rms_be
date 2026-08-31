package com.bob.db.entity;

import com.bob.db.enums.RequisitionEditStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_requisition_edit_requests", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_requisition_edit_requests SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobRequisitionEditRequestEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "job_requisition_edit_requests";

    @Column(name = "parent_requisition_id", nullable = false)
    private UUID parentRequisitionId;

    @Column(name = "request_type", length = 30)
    @Builder.Default
    private String requestType = "EDIT";

    @Column(name = "requisition_title")
    private String requisitionTitle;

    @Column(name = "requisition_description", columnDefinition = "text")
    private String requisitionDescription;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "requisition_status", length = 50)
    @Builder.Default
    private RequisitionEditStatus requisitionStatus = RequisitionEditStatus.DRAFT;

    @Column(name = "requisition_comments", columnDefinition = "text")
    private String requisitionComments;

    @Column(name = "indent_path", columnDefinition = "text")
    private String indentPath;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "base_version_no")
    @Builder.Default
    private Integer baseVersionNo = 1;

    @Column(name = "cutoff_date")
    private LocalDate cutoffDate;

    @OneToMany(mappedBy = "jobEditRequisition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobPositionEditRequestEntity> positionEditRequests = new ArrayList<>();
}
