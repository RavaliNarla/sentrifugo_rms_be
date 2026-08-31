package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "workflow_approval", schema = "recruitment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.workflow_approval SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class WorkflowApprovalEntity  extends BaseEntity<UUID>{

    public static final String ENTITY_TYPE = "workflow_approval";

//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    @Column(name = "approval_id")
//    private UUID approvalId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(name = "approver_role")
    private String approverRole;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "action")
    private String action;

    @Column(name = "action_date")
    private LocalDateTime actionDate;

    @Column(name = "comments")
    private String comments;

    @Column(name = "status")
    private String status;
}