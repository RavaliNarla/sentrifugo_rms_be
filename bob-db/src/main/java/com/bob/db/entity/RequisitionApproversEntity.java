package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "requisition_approvers", schema = "hr")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE hr.requisition_approvers SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RequisitionApproversEntity extends BaseEntity<UUID> {

    @Column(name = "approver_id", nullable = false, insertable = false, updatable = false)
    private UUID approverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", referencedColumnName = "id", nullable = false)
    private UserEntity approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false)
    private ApproverRole approverRole;

    public enum ApproverRole {
        L1,
        L2
    }
}
