package com.bob.db.repository;

import com.bob.db.entity.WorkflowApprovalEntity;

public interface WorkflowApprovalEntityRepositoryCustom {
    WorkflowApprovalEntity saveWithAudit(WorkflowApprovalEntity oldEntity,WorkflowApprovalEntity newEntity);
}

