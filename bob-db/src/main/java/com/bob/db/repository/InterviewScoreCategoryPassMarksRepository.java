package com.bob.db.repository;

import com.bob.db.entity.InterviewScoreCategoryPassMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewScoreCategoryPassMarksRepository extends JpaRepository<InterviewScoreCategoryPassMarksEntity, UUID> {
    Optional<InterviewScoreCategoryPassMarksEntity> findByReservationCategoryId(UUID reservationCategoryId);
}
