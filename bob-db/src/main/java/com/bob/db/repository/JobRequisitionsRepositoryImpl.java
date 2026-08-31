package com.bob.db.repository;

import com.bob.db.entity.AuditTrailEntity;
import com.bob.db.entity.DepartmentsEntity;
import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.entity.JobRequisitionEditRequestEntity;
import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.enums.RequisitionEditStatus;
import com.bob.db.enums.RequisitionStatus;
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
import java.time.LocalDateTime;
import java.util.*;

public class JobRequisitionsRepositoryImpl implements JobRequisitionsRepositoryCustom {

    private static final Logger logger = LoggerFactory.getLogger(JobRequisitionsRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AuditTrailEntityRepository auditTrailEntityRepository;

    @Override
    @Transactional
    public JobRequisitionsEntity saveWithAudit(JobRequisitionsEntity oldEntity, JobRequisitionsEntity newEntity) {
        JobRequisitionsEntity savedEntity = entityManager.merge(newEntity);
        // Fields to monitor for changes
        List<String> monitoredFields = Arrays.asList(
                DBConstants.REQUISITION_STATUS,
                DBConstants.REQUISITION_TITLE,
                DBConstants.REQUISITION_DESCRIPTION,
                DBConstants.START_DATE,
                DBConstants.END_DATE,
                DBConstants.REQUISITION_COMMENTS
        );
        List<AuditTrailEntity> audits = new ArrayList<>();
        for (String fieldName : monitoredFields) {
            try {
                Field field = JobRequisitionsEntity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                Object newValue = field.get(newEntity);
                boolean changed = (oldEntity == null && newValue != null) || (oldEntity != null && (oldValue == null ? newValue != null : !oldValue.equals(newValue)));
                if (changed) {
                    AuditTrailEntity audit = AuditTrailEntity.builder()
                            .changeType(oldEntity == null ? DBConstants.AUDIT_CHANGE_TYPE_CREATE : DBConstants.AUDIT_CHANGE_TYPE_UPDATE)
                            .entityId(savedEntity.getId())
                            .entityType(JobRequisitionsEntity.ENTITY_TYPE)
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
    public List<JobRequisitionsEntity> saveAllWithAudit(List<JobRequisitionsEntity> oldEntities, List<JobRequisitionsEntity> newEntities) {
        Map<Object, JobRequisitionsEntity> oldEntityMap = new HashMap<>();
        if (oldEntities != null) {
            for (JobRequisitionsEntity oldEntity : oldEntities) {
                if (oldEntity != null && oldEntity.getId() != null) {
                    oldEntityMap.put(oldEntity.getId(), oldEntity);
                }
            }
        }
        List<JobRequisitionsEntity> savedEntities = new ArrayList<>();
        List<AuditTrailEntity> allAudits = new ArrayList<>();
        List<String> monitoredFields = Arrays.asList(
                DBConstants.REQUISITION_STATUS,
                DBConstants.REQUISITION_TITLE,
                DBConstants.REQUISITION_DESCRIPTION,
                DBConstants.START_DATE,
                DBConstants.END_DATE,
                DBConstants.REQUISITION_COMMENTS
        );
        for (JobRequisitionsEntity newEntity : newEntities) {
            JobRequisitionsEntity oldEntity = newEntity.getId() != null ? oldEntityMap.get(newEntity.getId()) : null;
            JobRequisitionsEntity savedEntity = entityManager.merge(newEntity);
            savedEntities.add(savedEntity);
            for (String fieldName : monitoredFields) {
                try {
                    Field field = JobRequisitionsEntity.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                    Object newValue = field.get(newEntity);
                    boolean changed = (oldEntity == null && newValue != null) || (oldEntity != null && (oldValue == null ? newValue != null : !oldValue.equals(newValue)));
                    if (changed) {
                        AuditTrailEntity audit = AuditTrailEntity.builder()
                                .changeType(oldEntity == null ? DBConstants.AUDIT_CHANGE_TYPE_CREATE : DBConstants.AUDIT_CHANGE_TYPE_UPDATE)
                                .entityId(savedEntity.getId())
                                .entityType(JobRequisitionsEntity.ENTITY_TYPE)
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

    @Override
    public Page<JobRequisitionsEntity> findRequisitionsWithFilters(Integer year,Integer month, List<RequisitionStatus> statuses, String searchTerm,UUID departmentId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<JobRequisitionsEntity> query = cb.createQuery(JobRequisitionsEntity.class);
        Root<JobRequisitionsEntity> root = query.from(JobRequisitionsEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, query, year, month, statuses, searchTerm, departmentId);

        query.distinct(true);
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdDate")));

        TypedQuery<JobRequisitionsEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<JobRequisitionsEntity> results = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<JobRequisitionsEntity> countRoot = countQuery.from(JobRequisitionsEntity.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, countQuery, year, month, statuses, searchTerm, departmentId);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<JobRequisitionsEntity> root,
                                            CriteriaQuery<?> query,
                                            Integer year,Integer month, List<RequisitionStatus> statuses, String searchTerm,UUID departmentId) {
        List<Predicate> predicates = new ArrayList<>();

        // Always filter by is_active = true
        predicates.add(cb.isTrue(root.get("isActive")));

        // Year filter - compare created_date between start and end of year
        if (year != null) {
            LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
            LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
            Predicate startPredicate=cb.between(root.get(DBConstants.START_DATE), startOfYear, endOfYear);
//            Predicate endPredicate=cb.between(root.get("endDate"), startOfYear, endOfYear);
//            Predicate finalPredicate=cb.or(startPredicate,endPredicate);
            predicates.add(startPredicate);
        }

        if (month != null) {

            Predicate startDatePredicate = cb.equal(
                    cb.function("date_part", Integer.class, cb.literal("month"), root.get(DBConstants.START_DATE)),
                    month
            );
            predicates.add(startDatePredicate);
        }

        // Status filter.
        // For each requested status we also surface APPROVED main reqs that are in edit-mode
        // and whose active draft carries that same status — because from the user's perspective
        // the "effective status" of an edit-mode req is its draft's status.
        // The two groups are OR-ed so pagination stays correct (single query).
        if (statuses != null && !statuses.isEmpty()) {
            Predicate normalStatusPredicate = root.get(DBConstants.REQUISITION_STATUS).in(statuses);

            // Map to RequisitionEditStatus equivalents (same enum names where they overlap).
            List<RequisitionEditStatus> draftStatuses = statuses.stream()
                    .map(s -> {
                        try { return RequisitionEditStatus.valueOf(s.name()); }
                        catch (IllegalArgumentException ignored) { return null; }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            if (!draftStatuses.isEmpty()) {
                Subquery<Long> draftSubquery = query.subquery(Long.class);
                Root<JobRequisitionEditRequestEntity> draftRoot =
                        draftSubquery.from(JobRequisitionEditRequestEntity.class);
                draftSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(draftRoot.get("parentRequisitionId"), root.get("id")),
                                cb.isTrue(draftRoot.get("isActive")),
                                draftRoot.get("requisitionStatus").in(draftStatuses)
                        );
                Predicate editModeDraftPredicate = cb.and(
                        cb.isTrue(root.get("isInEditMode")),
                        cb.exists(draftSubquery)
                );
                predicates.add(cb.or(normalStatusPredicate, editModeDraftPredicate));
            } else {
                predicates.add(normalStatusPredicate);
            }
        }

        if (departmentId != null) {
            Subquery<Long> subQuery = query.subquery(Long.class);
            Root<JobPositionsEntity> posRoot = subQuery.from(JobPositionsEntity.class);

            Predicate reqToPos =
                    cb.equal(posRoot.get("requisitionId"), root.get("id"));

            Predicate deptFilter =
                    cb.equal(posRoot.get("deptId"), departmentId);


            subQuery.select(cb.literal(1L))
                    .where(reqToPos, deptFilter);


            predicates.add(cb.exists(subQuery));

        }

// ---- SEARCH FILTER ----
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String like = "%" + searchTerm.toLowerCase() + "%";

            Predicate titleLike =
                    cb.like(cb.lower(root.get(DBConstants.REQUISITION_TITLE)), like);

            Predicate codeLike =
                    cb.like(cb.lower(root.get("requisitionCode")), like);


            predicates.add(
                    cb.or(titleLike, codeLike)
            );

        }

        return predicates;
    }
}
