package com.bob.db.repository;

import com.bob.db.entity.AuditTrailEntity;
import com.bob.db.entity.WorkflowApprovalEntity;
import com.bob.db.util.DBConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class WorkflowApprovalEntityRepositoryImpl implements WorkflowApprovalEntityRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AuditTrailEntityRepository auditTrailEntityRepository;

    @Override
    @Transactional
    public WorkflowApprovalEntity saveWithAudit(WorkflowApprovalEntity oldEntity, WorkflowApprovalEntity newEntity) {
        WorkflowApprovalEntity savedEntity = entityManager.merge(newEntity);
            AuditTrailEntity audit = AuditTrailEntity.builder()
                    .changeType(oldEntity == null? DBConstants.AUDIT_CHANGE_TYPE_CREATE: DBConstants.AUDIT_CHANGE_TYPE_UPDATE)
                    .entityId(savedEntity.getEntityId())
                    .entityType(WorkflowApprovalEntity.ENTITY_TYPE)
                    .fieldChanged(DBConstants.AUDIT_CHANGE_FIELD_ACTION)
                    .newValue(newEntity.getAction())
                    .oldValue(oldEntity!=null?oldEntity.getAction():null)
                    .build();
            auditTrailEntityRepository.save(audit);
        return savedEntity;
    }

}
