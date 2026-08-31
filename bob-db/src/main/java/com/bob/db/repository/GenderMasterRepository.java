package com.bob.db.repository;

import com.bob.db.entity.GenderMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GenderMasterRepository extends JpaRepository<GenderMasterEntity, UUID> {
}
