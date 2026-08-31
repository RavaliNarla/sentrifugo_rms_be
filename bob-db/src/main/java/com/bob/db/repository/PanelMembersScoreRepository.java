package com.bob.db.repository;

import com.bob.db.entity.PanelMembersScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PanelMembersScoreRepository extends JpaRepository<PanelMembersScoreEntity, UUID> {

    List<PanelMembersScoreEntity> findAllByScheduledInterviewIdIn(List<UUID> ids);

    Optional<PanelMembersScoreEntity> findByApplicationIdAndScheduledInterviewIdAndCandidateIdAndPanelIdAndPanelMemberId(
            UUID applicationId, UUID scheduledInterviewId, UUID candidateId, UUID panelId, UUID panelMemberId);

    List<PanelMembersScoreEntity> findAllByApplicationIdAndScheduledInterviewIdAndCandidateIdAndPanelId(
            UUID applicationId, UUID scheduledInterviewId, UUID candidateId, UUID panelId);
}
