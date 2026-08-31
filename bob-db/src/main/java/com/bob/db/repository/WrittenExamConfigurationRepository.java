package com.bob.db.repository;

import com.bob.db.entity.WrittenExamConfigurationEntity;
import com.bob.db.enums.WrittenExamConfigurationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WrittenExamConfigurationRepository extends JpaRepository<WrittenExamConfigurationEntity, UUID> {
    Optional<WrittenExamConfigurationEntity> findByPositionId(UUID positionId);
    List<WrittenExamConfigurationEntity> findByPositionIdIn(List<UUID> positionIds);
    List<WrittenExamConfigurationEntity> findByStatusIn(List<WrittenExamConfigurationStatus> statuses);
    List<WrittenExamConfigurationEntity> findByPositionIdInAndStatusIn(List<UUID> positionIds, List<WrittenExamConfigurationStatus> statuses);
}
