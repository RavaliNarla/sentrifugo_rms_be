package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewPanelMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewPanelMemberRepository extends JpaRepository<InterviewPanelMemberEntity, UUID> {
    List<InterviewPanelMemberEntity> findByPanelId(UUID panelId);
    List<InterviewPanelMemberEntity> findByPanelIdIn(List<UUID> panelIds);
    void deleteByPanelId(UUID panelId);
}
