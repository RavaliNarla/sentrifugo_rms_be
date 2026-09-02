package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.ApproverRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "requisition_approvers", schema = "hr")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE hr.requisition_approvers SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RequisitionApproverEntity extends BaseEntity<UUID> {

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false)
    private ApproverRole approverRole;
}
