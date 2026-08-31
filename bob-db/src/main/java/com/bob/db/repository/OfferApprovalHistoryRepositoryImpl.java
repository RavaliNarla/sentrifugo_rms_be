package com.bob.db.repository;

import com.bob.db.entity.OfferApprovalHistoryEntity;
import com.bob.db.entity.WorkflowApprovalEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OfferApprovalHistoryRepositoryImpl implements OfferApprovalHistoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    private static final Logger logger = LoggerFactory.getLogger(OfferApprovalHistoryRepositoryImpl.class);


    public UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (UUID) auth.getPrincipal();
        } catch (Exception ex) {
            logger.error("Failed to get current user id: {}", ex.getMessage(), ex);
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    @Override
    @Transactional
    public OfferApprovalHistoryEntity saveWithWorkflow(OfferApprovalHistoryEntity newEntity,String comments) {
        OfferApprovalHistoryEntity savedEntity = entityManager.merge(newEntity);

        try {
            WorkflowApprovalEntity workflow = WorkflowApprovalEntity.builder()
                    .entityId(savedEntity.getId())
                    .entityType(OfferApprovalHistoryEntity.ENTITY_TYPE)
                    .stepNumber(1)
                    .approverRole("")
                    .approverId(getCurrentUserId())
                    .action(savedEntity.getApprovalStatus().toString())
                    .actionDate(LocalDateTime.now())
                    .comments(comments)
                    .status(savedEntity.getApprovalStatus().toString())
                    .build();

            workflowApprovalEntityRepository.save(workflow);
        } catch (Exception ex) {
            logger.error("Failed to save workflow approval for offerApprovalHistoryId {}: {}", savedEntity.getId(), ex.getMessage(), ex);
        }

        return savedEntity;
    }

    @Override
    @Transactional
    public List<OfferApprovalHistoryEntity> saveAllWithWorkflow(List<OfferApprovalHistoryEntity> newEntities,String comments) {
        List<WorkflowApprovalEntity> workflows = new ArrayList<>();

        // Merge all entities into the persistence context
        List<OfferApprovalHistoryEntity> savedEntities = newEntities.stream()
                .map(entityManager::merge)
                .collect(Collectors.toList());

        // Generate workflow audit records
        for (OfferApprovalHistoryEntity savedEntity : savedEntities) {
            WorkflowApprovalEntity workflow = WorkflowApprovalEntity.builder()
                    .entityId(savedEntity.getId())
                    .entityType(OfferApprovalHistoryEntity.ENTITY_TYPE)
                    .stepNumber(1)
                    .approverRole("")
                    .approverId(getCurrentUserId())
                    .action(savedEntity.getApprovalStatus().toString())
                    .actionDate(LocalDateTime.now())
                    .comments(comments)
                    .status(savedEntity.getApprovalStatus().toString())
                    .build();

            workflows.add(workflow);
        }

        // Batch save workflows
        workflowApprovalEntityRepository.saveAll(workflows);

        return savedEntities;
    }
}