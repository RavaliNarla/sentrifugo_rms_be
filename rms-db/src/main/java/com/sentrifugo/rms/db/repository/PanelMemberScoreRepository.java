package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.PanelMemberScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PanelMemberScoreRepository extends JpaRepository<PanelMemberScoreEntity, UUID> {
    List<PanelMemberScoreEntity> findByCandidateId(UUID candidateId);
    Optional<PanelMemberScoreEntity> findByCandidateIdAndPanelMemberId(UUID candidateId, UUID panelMemberId);
}
