package com.bob.db.repository;

import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.InterviewSchedulingStatus;
import com.bob.db.util.DBConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.*;

public class InterviewScheduleRepositoryImpl implements InterviewScheduleRepositoryCustom{

    private static final Logger logger = LoggerFactory.getLogger(InterviewScheduleRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AuditTrailEntityRepository auditTrailEntityRepository;

    @Override
    public Page<InterviewScheduleEntity> findInterviewSchedulingsWithFilters(
            String searchText,
            List<InterviewSchedulingStatus> statusList,
            List<UUID> positionIds,  // Add this parameter
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // DATA QUERY
        CriteriaQuery<InterviewScheduleEntity> cq = cb.createQuery(InterviewScheduleEntity.class);
        Root<InterviewScheduleEntity> root = cq.from(InterviewScheduleEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        buildPredicates(cb, root, cq, predicates, searchText, statusList, positionIds);

        Join<InterviewScheduleEntity, InterviewCentresEntity> centreJoin =
                root.join("interviewCentre", JoinType.LEFT);

        Join<InterviewScheduleEntity,InterviewPanelsEntity> panelsEntityJoin=root.join("interviewPanels",JoinType.LEFT);

        Root<CandidateApplicationsEntity> appRoot =
                cq.getRoots()
                        .stream()
                        .filter(r ->
                                r.getJavaType()
                                        .equals(CandidateApplicationsEntity.class)
                        )
                        .map(r -> (Root<CandidateApplicationsEntity>) r)
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "CandidateApplicationsEntity root not found"
                                )
                        );

        CriteriaBuilder.Case<Integer> positionOrderCase =
                cb.selectCase();

        if (positionIds != null && !positionIds.isEmpty()) {

            for (int i = 0; i < positionIds.size(); i++) {

                positionOrderCase.when(
                        cb.equal(
                                appRoot.get("positionId"),
                                positionIds.get(i)
                        ),
                        i
                );
            }
        }

        cq.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(
                        cb.asc(
                                positionOrderCase.otherwise(
                                        positionIds != null
                                                ? positionIds.size()
                                                : Integer.MAX_VALUE
                                )
                        ),
//                        cb.asc(centreJoin.get("displayName")),
//                        cb.asc(panelsEntityJoin.get("panelName")),
                        cb.asc(root.get("interviewStartAt"))
                );

        TypedQuery<InterviewScheduleEntity> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<InterviewScheduleEntity> results = typedQuery.getResultList();

        // COUNT QUERY
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<InterviewScheduleEntity> countRoot = countCq.from(InterviewScheduleEntity.class);
        List<Predicate> countPredicates = new ArrayList<>();

        buildPredicates(cb, countRoot, countCq, countPredicates, searchText, statusList, positionIds);

        countCq.select(cb.count(countRoot))
                .where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    public void buildPredicates(
            CriteriaBuilder cb,
            Root<InterviewScheduleEntity> root,
            CriteriaQuery<?> query,
            List<Predicate> predicates,
            String searchText,
            List<InterviewSchedulingStatus> statusList,
            List<UUID> positionIds  ) {  // Add this parameter

        predicates.add(cb.isTrue(root.get("isActive")));

        // Manual Joins (Multiple Roots)
            Root<CandidateApplicationsEntity> appRoot = query.from(CandidateApplicationsEntity.class);
            Root<CandidateProfileEntity> profileRoot = query.from(CandidateProfileEntity.class);

        // Cross Join Conditions (ON clause)
        predicates.add(cb.equal(root.get("applicationId"), appRoot.get("id")));
        predicates.add(cb.equal(appRoot.get("candidateId"), profileRoot.get("candidateId")));

        // Position Filter
        if (positionIds != null && !positionIds.isEmpty()) {
            predicates.add(appRoot.get("positionId").in(positionIds));
        }

        // Search Logic
        if (searchText != null && !searchText.trim().isEmpty()) {
            String like = "%" + searchText.toLowerCase() + "%";
            Expression<String> fullNameExpr = cb.concat(
                    cb.lower(profileRoot.get("firstName")),
                    cb.concat(
                            " ",
                            cb.concat(
                                    cb.coalesce(cb.lower(profileRoot.get("middleName")), ""),
                                    cb.concat(
                                            " ",
                                            cb.lower(profileRoot.get("lastName"))
                                    )
                            )
                    ));
            predicates.add(cb.like(fullNameExpr, like));

        }

        if (statusList != null && !statusList.isEmpty()) {
            predicates.add(root.get(DBConstants.INTERVIEW_STATUS).in(statusList));
        }
    }

    @Override
    @Transactional
    public InterviewScheduleEntity saveWithAudit(InterviewScheduleEntity oldEntity, InterviewScheduleEntity newEntity) {
        InterviewScheduleEntity savedEntity = entityManager.merge(newEntity);
        
        // Fields to monitor for changes
        List<String> monitoredFields = Arrays.asList(
                "zonalVerificationStatus",
                DBConstants.INTERVIEW_STATUS,
                "lptRequired",
                "lptStatus"
        );
        
        List<AuditTrailEntity> audits = new ArrayList<>();
        for (String fieldName : monitoredFields) {
            try {
                Field field = InterviewScheduleEntity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                Object newValue = field.get(newEntity);
                
                boolean changed = (oldEntity == null && newValue != null) || 
                                (oldEntity != null && (oldValue == null ? newValue != null : !oldValue.equals(newValue)));
                
                if (changed) {
                    AuditTrailEntity audit = AuditTrailEntity.builder()
                            .changeType(oldEntity == null ? DBConstants.AUDIT_CHANGE_TYPE_CREATE : DBConstants.AUDIT_CHANGE_TYPE_UPDATE)
                            .entityId(savedEntity.getId())
                            .entityType(InterviewScheduleEntity.ENTITY_TYPE)
                            .fieldChanged(fieldName)
                            .newValue(newValue != null ? newValue.toString() : null)
                            .oldValue(oldValue != null ? oldValue.toString() : null)
                            .build();
                    audits.add(audit);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Error accessing field '{}' for audit: {}", fieldName, e.getMessage(), e);
            }
        }
        
        if (!audits.isEmpty()) {
            auditTrailEntityRepository.saveAll(audits);
        }
        
        return savedEntity;
    }

    @Override
    @Transactional
    public List<InterviewScheduleEntity> saveAllWithAudit(List<InterviewScheduleEntity> oldEntities, List<InterviewScheduleEntity> newEntities) {
        Map<Object, InterviewScheduleEntity> oldEntityMap = new HashMap<>();
        if (oldEntities != null) {
            for (InterviewScheduleEntity oldEntity : oldEntities) {
                if (oldEntity != null && oldEntity.getId() != null) {
                    oldEntityMap.put(oldEntity.getId(), oldEntity);
                }
            }
        }
        
        List<InterviewScheduleEntity> savedEntities = new ArrayList<>();
        List<AuditTrailEntity> allAudits = new ArrayList<>();
        
        List<String> monitoredFields = Arrays.asList(
                "zonalVerificationStatus",
                DBConstants.INTERVIEW_STATUS,
                "lptRequired",
                "lptStatus"
        );
        
        for (InterviewScheduleEntity newEntity : newEntities) {
            InterviewScheduleEntity oldEntity = newEntity.getId() != null ? oldEntityMap.get(newEntity.getId()) : null;
            InterviewScheduleEntity savedEntity = entityManager.merge(newEntity);
            savedEntities.add(savedEntity);
            
            for (String fieldName : monitoredFields) {
                try {
                    Field field = InterviewScheduleEntity.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                    Object newValue = field.get(newEntity);
                    
                    boolean changed = (oldEntity == null && newValue != null) || 
                                    (oldEntity != null && (oldValue == null ? newValue != null : !oldValue.equals(newValue)));
                    
                    if (changed) {
                        AuditTrailEntity audit = AuditTrailEntity.builder()
                                .changeType(oldEntity == null ? DBConstants.AUDIT_CHANGE_TYPE_CREATE : DBConstants.AUDIT_CHANGE_TYPE_UPDATE)
                                .entityId(savedEntity.getId())
                                .entityType(InterviewScheduleEntity.ENTITY_TYPE)
                                .fieldChanged(fieldName)
                                .newValue(newValue != null ? newValue.toString() : null)
                                .oldValue(oldValue != null ? oldValue.toString() : null)
                                .build();
                        allAudits.add(audit);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.error("Error accessing field '{}' for audit: {}", fieldName, e.getMessage(), e);
                }
            }
        }
        
        if (!allAudits.isEmpty()) {
            auditTrailEntityRepository.saveAll(allAudits);
        }
        
        return savedEntities;
    }

}
