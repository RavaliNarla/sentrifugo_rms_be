package com.bob.db.repository;

import com.bob.db.entity.LanguageMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LanguageMasterRepository extends JpaRepository<LanguageMasterEntity, UUID> {
}
