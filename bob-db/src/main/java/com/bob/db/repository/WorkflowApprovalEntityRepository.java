package com.bob.db.repository;

import com.bob.db.dto.WorkflowApprovalDTO;
import com.bob.db.entity.WorkflowApprovalEntity;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WorkflowApprovalEntityRepository extends JpaRepository<WorkflowApprovalEntity, UUID>,WorkflowApprovalEntityRepositoryCustom {
    List<WorkflowApprovalEntity> findByEntityTypeAndEntityIdOrderByActionDateDesc(String entityType, UUID entityId);

    List<WorkflowApprovalEntity> findByEntityTypeAndEntityIdInOrderByActionDateDesc(String entityType, Collection<UUID> entityIds);

    @Query("""
    SELECT wa
    FROM WorkflowApprovalEntity wa
    WHERE wa.entityType = :entityType
      AND wa.entityId IN :entityIds
      AND wa.status = :status
      AND wa.createdDate = (
          SELECT MAX(sub.createdDate)
          FROM WorkflowApprovalEntity sub
          WHERE sub.entityId = wa.entityId
            AND sub.entityType = wa.entityType
            AND sub.status = wa.status
      )
    ORDER BY wa.createdDate DESC
""")
    List<WorkflowApprovalEntity> findLatestByEntityIds(
            @Param("entityType") String entityType,
            @Param("entityIds") List<UUID> entityIds,
            @Param("status") String status
    );
}