package com.bob.db.repository;

import com.bob.db.entity.EmployementTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployementTypesRepository extends JpaRepository<EmployementTypesEntity, UUID> {
}
