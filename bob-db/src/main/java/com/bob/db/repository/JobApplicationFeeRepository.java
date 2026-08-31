package com.bob.db.repository;

import com.bob.db.entity.JobApplicationFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobApplicationFeeRepository extends JpaRepository<JobApplicationFeeEntity,UUID> {

    // Custom query to find JobApplicationFee by positionId

    JobApplicationFeeEntity findByPositionId(UUID positionId);


}
