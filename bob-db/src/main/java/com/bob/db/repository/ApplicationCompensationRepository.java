package com.bob.db.repository;

import com.bob.db.entity.ApplicationCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationCompensationRepository extends JpaRepository<ApplicationCompensationEntity, UUID> {
    ApplicationCompensationEntity findByApplicationId(UUID applicationId);
}

