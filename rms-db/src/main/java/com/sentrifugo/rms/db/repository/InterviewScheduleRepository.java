package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewScheduleRepository extends JpaRepository<InterviewScheduleEntity, UUID> {
    Optional<InterviewScheduleEntity> findByCandidateId(UUID candidateId);
    List<InterviewScheduleEntity> findByCandidateIdIn(List<UUID> candidateIds);
    List<InterviewScheduleEntity> findByPanelIdAndInterviewDate(UUID panelId, LocalDate interviewDate);
}
