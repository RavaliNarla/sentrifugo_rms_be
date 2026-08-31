package com.bob.db.repository;

import com.bob.db.entity.MaritalStatusMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaritalStatusMasterRepository extends JpaRepository<MaritalStatusMasterEntity, UUID> {
}
