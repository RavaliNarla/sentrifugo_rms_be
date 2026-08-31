package com.bob.db.repository;

import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.WorkflowApprovalEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CandidateApplicationsRepositoryImpl implements CandidateApplicationsRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    public UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return  (UUID) auth.getPrincipal();
        }catch (Exception ex){
            logger.error("Failed to get current user id: {}", ex.getMessage(), ex);
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CandidateApplicationsRepositoryImpl.class);

    @Override
    @Transactional
    public CandidateApplicationsEntity saveWithWorkflow(CandidateApplicationsEntity newEntity) {
        CandidateApplicationsEntity savedEntity = entityManager.merge(newEntity);

        try {
            WorkflowApprovalEntity workflow = WorkflowApprovalEntity.builder()
                    .entityId(savedEntity.getId())
                    .entityType(CandidateApplicationsEntity.ENTITY_TYPE)
                    .stepNumber(1) // Assuming step 1 for now
                    .approverRole("")
                    .approverId(getCurrentUserId())//TODO: once security utils is migrated to common-util populate this
                    .action(newEntity.getApplicationStatus().toString())
                    .actionDate(LocalDateTime.now())
                    .comments("Status updated to " + newEntity.getApplicationStatus())
                    .status(newEntity.getApplicationStatus().toString())
                    .build();
            workflowApprovalEntityRepository.save(workflow);
        } catch (Exception ex) {
            logger.error("Failed to save workflow approval for applicationId {}: {}", savedEntity.getId(), ex.getMessage(), ex);
        }

        return savedEntity;
    }

    @Override
    @Transactional
    public List<CandidateApplicationsEntity> saveAllWithWorkflow(List<CandidateApplicationsEntity> newEntities) {

        List<WorkflowApprovalEntity> workflows = new ArrayList<>();

        List<CandidateApplicationsEntity> savedEntities =
                newEntities.stream()
                        .map(entityManager::merge)
                        .collect(Collectors.toList());

        for (CandidateApplicationsEntity savedEntity : savedEntities) {

            WorkflowApprovalEntity workflow = WorkflowApprovalEntity.builder()
                    .entityId(savedEntity.getId())
                    .entityType(CandidateApplicationsEntity.ENTITY_TYPE)
                    .stepNumber(1)
                    .approverRole("")
                    .approverId(getCurrentUserId())
                    .action(savedEntity.getApplicationStatus().toString())
                    .actionDate(LocalDateTime.now())
                    .comments("Status updated to " + savedEntity.getApplicationStatus())
                    .status(savedEntity.getApplicationStatus().toString())
                    .build();

            workflows.add(workflow);
        }

        workflowApprovalEntityRepository.saveAll(workflows);

        return savedEntities;
    }

}
