package com.bob.db.repository;

import com.bob.db.entity.MedicalCentresEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MedicalCentresRepository extends JpaRepository<MedicalCentresEntity, UUID> {
}
