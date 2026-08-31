package com.bob.db.repository;

import com.bob.db.entity.PositionPanelEntity;
import com.bob.db.enums.PositionPanelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public interface PositionPanelRepository extends JpaRepository<PositionPanelEntity, UUID> , JpaSpecificationExecutor<PositionPanelEntity> {
    List<PositionPanelEntity> findByJobPositionId(UUID jobPositionId);

    List<PositionPanelEntity> findByJobPositionIdAndPositionPanelStatus(UUID jobPositionId, PositionPanelStatus status);

    List<PositionPanelEntity> findByJobPosition_IdAndEndDateGreaterThanEqualAndPositionPanelStatus(
            UUID jobPositionId,
            LocalDate date2,
            PositionPanelStatus status
    );


    List<PositionPanelEntity> findByJobPositionIdInAndPositionPanelStatus(
            List<UUID> jobPositionIds,
            PositionPanelStatus status
    );

    List<PositionPanelEntity> findByJobPosition_IdInAndEndDateGreaterThanEqualAndPositionPanelStatus(
            List<UUID> jobPositionIds,
            LocalDate date2,
            PositionPanelStatus status
    );

    List<PositionPanelEntity> findByJobPosition_IdInAndEndDateGreaterThanEqual(
            List<UUID> jobPositionIds,
            LocalDate date
    );
    List<PositionPanelEntity> findByInterviewPanel_Id(UUID panelId);

    List<PositionPanelEntity> findByJobPositionIdNotAndInterviewPanel_PanelMembers_PanelMember_IdIn(UUID jobPositionId, Set<UUID> panelMemberIds);

    List<PositionPanelEntity> findByJobPositionIdIn(Collection<UUID> positionIds);

    @Query("""
    SELECT p
    FROM PositionPanelEntity p
    JOIN FETCH p.interviewPanel ip
    JOIN FETCH ip.committee c
    WHERE ip.id IN :panelIds
    AND p.positionPanelStatus IN :posPanelStatus
    AND :currentDate BETWEEN p.startDate AND p.endDate
""")
    List<PositionPanelEntity> findActivePanels(
            Collection<UUID> panelIds,
            LocalDate currentDate,
            List<PositionPanelStatus> posPanelStatus
    );

    List<PositionPanelEntity> findByInterviewPanel_PanelMembers_PanelMember_IdIn(Set<UUID> userIds);

    List<PositionPanelEntity>
    findByJobPositionIdAndInterviewPanel_PanelMembers_PanelMember_IdAndInterviewPanel_Committee_CommitteeName(UUID positionId, UUID userId, String committeeName);

    List<PositionPanelEntity> findByInterviewPanel_IdIn(Set<UUID> panelIds);
}