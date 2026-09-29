package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.InterviewRoundMetaEntity;
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
public interface InterviewRoundMetaRepository extends JpaRepository<InterviewRoundMetaEntity, UUID> {

    Optional<InterviewRoundMetaEntity> findByCandidateIdAndRound(UUID candidateId, Integer round);

    List<InterviewRoundMetaEntity> findByCandidateId(UUID candidateId);

    boolean existsByPanelId(UUID panelId);

    @Query("""
            SELECT m FROM InterviewRoundMetaEntity m
            WHERE m.panelId = :panelId
              AND m.interviewDate >= :from
              AND m.interviewDate <= :to
            ORDER BY m.interviewDate ASC, m.startTime ASC
            """)
    Page<InterviewRoundMetaEntity> findByPanelAndDateRange(
            @Param("panelId") UUID panelId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT m FROM InterviewRoundMetaEntity m
            WHERE m.panelId IN :panelIds
              AND m.interviewDate >= :from
              AND m.interviewDate <= :to
            ORDER BY m.interviewDate ASC, m.startTime ASC
            """)
    Page<InterviewRoundMetaEntity> findByPanelIdsAndDateRange(
            @Param("panelIds") Collection<UUID> panelIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
