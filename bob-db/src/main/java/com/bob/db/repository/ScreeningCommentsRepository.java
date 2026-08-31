package com.bob.db.repository;

import com.bob.db.entity.ScreeningCommentsEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreeningCommentsRepository extends JpaRepository<ScreeningCommentsEntity, UUID> {
    List<ScreeningCommentsEntity> findAllByApplicationId(UUID applicationId, Sort sort);
}

