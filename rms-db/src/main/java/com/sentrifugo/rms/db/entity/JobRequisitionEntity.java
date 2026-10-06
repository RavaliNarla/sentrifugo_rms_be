package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.RequisitionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_requisitions", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.job_requisitions SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobRequisitionEntity extends BaseEntity<UUID> {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_fulfilment_date")
    private LocalDate expectedFulfilmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RequisitionStatus status = RequisitionStatus.NEW;

    @Column(name = "requisition_code", unique = true)
    private String requisitionCode;

    /** Optional: when set, positions under this requisition inherit/lock this department. */
    @Column(name = "department_id")
    private UUID departmentId;

    /** Optional: when set, positions under this requisition inherit/lock this location. */
    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
