package com.bob.db.repository;

import com.bob.db.entity.CertificationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CertificationMasterRepository extends JpaRepository<CertificationMasterEntity, UUID> {
    CertificationMasterEntity findByCertificationNameIgnoreCase(String other);
}
