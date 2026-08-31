package com.bob.db.repository;

import com.bob.db.entity.JobAgeRelaxationsEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobAgeRelaxationsRepository extends JpaRepository<JobAgeRelaxationsEntity,UUID> {


    @Transactional
    JobAgeRelaxationsEntity findByPositionId(UUID positionId);

}