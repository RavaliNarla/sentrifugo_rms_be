package com.bob.db.repository;

import com.bob.db.entity.TemplatesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<TemplatesEntity, UUID> {
}
