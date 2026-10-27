package com.sentrifugo.rms.common.audit;

import com.sentrifugo.rms.db.audit.AuditCorrelation;
import com.sentrifugo.rms.db.audit.AuditHook;
import com.sentrifugo.rms.db.audit.AuditHooks;
import com.sentrifugo.rms.db.audit.AuditSnapshot;
import com.sentrifugo.rms.db.entity.AuditEventEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.AuditEventRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Field-level audit writer for {@code hr.audit_events}.
 * One DB row per changed field; shared {@code event_id} per entity mutation;
 * shared {@code correlation_id} per HTTP request / job.
 *
 * <p>Only the recruitment / master / user entities listed in {@link #AUDITED_ENTITY_TYPES}
 * are recorded. Side effects such as Notification rows are skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService implements AuditHook {

    /**
     * Scoped coverage: users, masters, requisitions/positions, candidates, offers,
     * interviews, and approval history. Not every BaseEntity.
     */
    private static final Set<String> AUDITED_ENTITY_TYPES = Set.of(
            // Users
            "User",
            // Master data
            "Department",
            "Location",
            "PositionTitle",
            "Certification",
            "EducationQualification",
            "Specialization",
            "ApprovedByRole",
            "OfferTemplate",
            "State",
            // Requisitions / positions
            "JobRequisition",
            "JobPosition",
            // Approvals
            "RequisitionApprovalHistory",
            "OfferApprovalHistory",
            // Candidates / offers
            "Candidate",
            "CandidateOffer",
            // Interviews
            "InterviewSchedule",
            "InterviewRoundMeta",
            "InterviewPanel",
            "InterviewPanelMember",
            "PanelMemberScore"
    );

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${spring.application.name:rms}")
    private String portalName;

    private TransactionTemplate requiresNewTx;

    @PostConstruct
    void registerHook() {
        requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AuditHooks.register(this);
    }

    @Override
    public void onCreate(Object entity, Map<String, Object> after) {
        recordEntityChange(entity, "CREATE", null, after);
    }

    @Override
    public void onUpdate(Object entity, Map<String, Object> before, Map<String, Object> after) {
        recordEntityChange(entity, "UPDATE", before, after);
    }

    @Override
    public void onDelete(Object entity, Map<String, Object> before) {
        recordEntityChange(entity, "DELETE", before, null);
    }

    public void recordEntityChange(Object entity, String action, Map<String, Object> before, Map<String, Object> after) {
        if (entity == null) {
            return;
        }

        String entityType = AuditSnapshot.entityTypeName(entity);
        if (!AUDITED_ENTITY_TYPES.contains(entityType)) {
            return;
        }
        UUID entityId = AuditSnapshot.entityId(entity);

        String effectiveAction = action;
        if ("UPDATE".equals(action) && before != null && after != null) {
            Object statusFrom = before.get("status");
            Object statusTo = after.get("status");
            if (statusFrom != null && statusTo != null && !statusFrom.equals(statusTo)) {
                effectiveAction = "STATUS_CHANGE";
            }
            Object activeFrom = before.get("isActive");
            Object activeTo = after.get("isActive");
            if (Boolean.TRUE.equals(activeFrom) && Boolean.FALSE.equals(activeTo)) {
                effectiveAction = "DEACTIVATE";
            } else if (Boolean.FALSE.equals(activeFrom) && Boolean.TRUE.equals(activeTo)) {
                effectiveAction = "REACTIVATE";
            }
        }

        List<Map<String, Object>> fieldRows;
        if ("CREATE".equals(effectiveAction)) {
            fieldRows = AuditSnapshot.fieldsForCreate(after);
        } else if ("DELETE".equals(effectiveAction)) {
            fieldRows = AuditSnapshot.fieldsForDelete(before);
        } else {
            fieldRows = AuditSnapshot.diff(before, after);
        }

        if (fieldRows == null || fieldRows.isEmpty()) {
            if ("UPDATE".equals(effectiveAction)) {
                return;
            }
            // Still record the entity event when we have no field detail.
            java.util.Map<String, Object> sentinel = new java.util.LinkedHashMap<>();
            sentinel.put("field", "*");
            sentinel.put("from", null);
            sentinel.put("to", null);
            fieldRows = List.of(sentinel);
        }

        String summary = buildSummary(effectiveAction, entityType, entityId, before, after, fieldRows);
        Actor actor = resolveActor();
        UUID correlationId = AuditCorrelation.getOrCreate();
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        List<PendingFieldRow> pendingRows = new ArrayList<>(fieldRows.size());
        for (Map<String, Object> change : fieldRows) {
            pendingRows.add(new PendingFieldRow(
                    correlationId,
                    eventId,
                    occurredAt,
                    actor.id(),
                    actor.name(),
                    actor.email(),
                    effectiveAction,
                    entityType,
                    entityId,
                    String.valueOf(change.get("field")),
                    nullToEmpty(change.get("from")),
                    nullToEmpty(change.get("to")),
                    portalName,
                    summary
            ));
        }
        enqueue(pendingRows, effectiveAction, entityType, entityId);
    }

    private static String nullToEmpty(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isEmpty() ? null : s;
    }

    private void enqueue(List<PendingFieldRow> rows, String action, String entityType, UUID entityId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistSafe(rows, action, entityType, entityId);
                }
            });
        } else {
            persistSafe(rows, action, entityType, entityId);
        }
    }

    private void persistSafe(List<PendingFieldRow> rows, String action, String entityType, UUID entityId) {
        try {
            requiresNewTx.executeWithoutResult(status -> {
                List<AuditEventEntity> entities = new ArrayList<>(rows.size());
                for (PendingFieldRow row : rows) {
                    entities.add(AuditEventEntity.builder()
                            .correlationId(row.correlationId())
                            .eventId(row.eventId())
                            .occurredAt(row.occurredAt())
                            .actorId(row.actorId())
                            .actorName(row.actorName())
                            .actorEmail(row.actorEmail())
                            .action(row.action())
                            .entityType(row.entityType())
                            .entityId(row.entityId())
                            .fieldName(row.fieldName())
                            .beforeValue(row.beforeValue())
                            .afterValue(row.afterValue())
                            .portal(row.portal())
                            .summary(row.summary())
                            .build());
                }
                auditEventRepository.saveAll(entities);
            });
        } catch (Exception e) {
            log.warn("Audit write failed for {} {} {} ({} field rows): {}",
                    action, entityType, entityId, rows.size(), e.getMessage());
        }
    }

    private String buildSummary(
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> before,
            Map<String, Object> after,
            List<Map<String, Object>> fieldRows
    ) {
        String label = labelOf(after != null ? after : before);
        StringBuilder sb = new StringBuilder();
        sb.append(action).append(' ').append(entityType);
        if (label != null) {
            sb.append(" '").append(label).append('\'');
        }
        if (entityId != null) {
            sb.append(" (").append(entityId).append(')');
        }
        if ("STATUS_CHANGE".equals(action) && before != null && after != null) {
            sb.append(": ").append(before.get("status")).append(" → ").append(after.get("status"));
        } else if (fieldRows != null && !fieldRows.isEmpty() && fieldRows.size() <= 6
                && !"*".equals(fieldRows.get(0).get("field"))) {
            sb.append(": ");
            for (int i = 0; i < fieldRows.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Map<String, Object> c = fieldRows.get(i);
                sb.append(c.get("field")).append('=').append(c.get("to"));
            }
        } else if (fieldRows != null && fieldRows.size() > 6) {
            sb.append(": ").append(fieldRows.size()).append(" fields changed");
        }
        return sb.toString();
    }

    private static String labelOf(Map<String, Object> snap) {
        if (snap == null) {
            return null;
        }
        for (String key : List.of("name", "email", "code", "title", "roundName")) {
            Object v = snap.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        return null;
    }

    private Actor resolveActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return new Actor(null, "anonymous", null);
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof UUID id) {
                UserEntity user = userRepository.findById(id).orElse(null);
                if (user != null) {
                    return new Actor(user.getId(), user.getName(), user.getEmail());
                }
                return new Actor(id, id.toString(), null);
            }
            return new Actor(null, String.valueOf(principal), null);
        } catch (Exception e) {
            return new Actor(null, "unknown", null);
        }
    }

    private record Actor(UUID id, String name, String email) {
    }

    private record PendingFieldRow(
            UUID correlationId,
            UUID eventId,
            Instant occurredAt,
            UUID actorId,
            String actorName,
            String actorEmail,
            String action,
            String entityType,
            UUID entityId,
            String fieldName,
            String beforeValue,
            String afterValue,
            String portal,
            String summary
    ) {
    }
}
