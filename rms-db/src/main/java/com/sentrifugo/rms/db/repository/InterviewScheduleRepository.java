package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewScheduleEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
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

    Optional<InterviewScheduleEntity> findByAcceptToken(UUID acceptToken);

    boolean existsBySupersededTokensContaining(String token);

    boolean existsByPanelId(UUID panelId);

    /** Positions that have at least one candidate in the given statuses scheduled on one of these panels. */
    @Query("""
            SELECT DISTINCT c.positionId FROM InterviewScheduleEntity s, CandidateEntity c
            WHERE c.id = s.candidateId
              AND s.panelId IN :panelIds
              AND c.status IN :statuses
            """)
    List<UUID> findPositionIdsForPanels(
            @Param("panelIds") Collection<UUID> panelIds,
            @Param("statuses") Collection<CandidateStatus> statuses);

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
