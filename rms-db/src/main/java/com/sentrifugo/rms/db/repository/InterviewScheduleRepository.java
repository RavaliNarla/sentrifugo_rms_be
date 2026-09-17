package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewScheduleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewScheduleRepository extends JpaRepository<InterviewScheduleEntity, UUID> {
    Optional<InterviewScheduleEntity> findByCandidateId(UUID candidateId);
    List<InterviewScheduleEntity> findByCandidateIdIn(List<UUID> candidateIds);
    List<InterviewScheduleEntity> findByPanelIdAndInterviewDate(UUID panelId, LocalDate interviewDate);

    List<InterviewScheduleEntity> findByPanelIdInAndInterviewDate(Collection<UUID> panelIds, LocalDate interviewDate);

    boolean existsByPanelId(UUID panelId);

    @Query("""
            SELECT s FROM InterviewScheduleEntity s
            WHERE s.panelId = :panelId
              AND s.interviewDate >= :from
              AND s.interviewDate <= :to
            ORDER BY s.interviewDate ASC, s.startTime ASC
            """)
    Page<InterviewScheduleEntity> findByPanelAndDateRange(
            @Param("panelId") UUID panelId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT s FROM InterviewScheduleEntity s
            WHERE s.panelId IN :panelIds
              AND s.interviewDate >= :from
              AND s.interviewDate <= :to
            ORDER BY s.interviewDate ASC, s.startTime ASC
            """)
    Page<InterviewScheduleEntity> findByPanelIdsAndDateRange(
            @Param("panelIds") Collection<UUID> panelIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
