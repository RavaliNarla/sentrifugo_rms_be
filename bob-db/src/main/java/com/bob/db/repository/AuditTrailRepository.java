package com.bob.db.repository;

import com.bob.db.entity.AuditTrailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrailEntity, UUID> {
    // Add custom query methods if needed
}