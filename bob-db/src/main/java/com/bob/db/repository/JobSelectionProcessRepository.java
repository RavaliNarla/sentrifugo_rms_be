package com.bob.db.repository;

import com.bob.db.entity.JobSelectionProcessEntity;
import io.micrometer.common.KeyValues;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface JobSelectionProcessRepository extends JpaRepository<JobSelectionProcessEntity,UUID> {

    JobSelectionProcessEntity findByPositionId(UUID positionId);

    //update by position id
    @Modifying
    @Transactional
    @Query(value = "UPDATE job_selection_process SET selection_procedure = ?2 WHERE position_id = ?1", nativeQuery = true)
    int updateByPositionId(UUID positionId, String selectionProcess);

    List<JobSelectionProcessEntity> findByPositionIdIn(List<UUID> positionIds);
}
