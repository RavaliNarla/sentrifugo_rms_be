package com.bob.db.repository;

import com.bob.db.entity.InterviewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewsRepository  extends JpaRepository<InterviewsEntity,UUID>,InterviewsRepositoryCustom {

    List<InterviewsEntity> findByScheduledAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Optional<InterviewsEntity> findByApplicationId(UUID applicationId);


    List<InterviewsEntity> findAllByinterviewerIdAndScheduledAtBetweenAndIsPanelInterviewTrue(
            UUID panelId, LocalDateTime start, LocalDateTime end);
    List<InterviewsEntity> findByScheduledAtBetweenAndStatusIn(LocalDateTime startDate, LocalDateTime endDate, List<String> relevantStatuses);

    List<InterviewsEntity> findAllByInterviewerIdInAndIsPanelInterviewTrueAndScheduledAtGreaterThanEqual(
            List<UUID> panelIds, LocalDateTime startDateTime);
}
