package com.bob.db.repository;

import com.bob.db.entity.RelaxationTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RelaxationTypesRepository extends JpaRepository<RelaxationTypesEntity, UUID> {

    List<RelaxationTypesEntity> findAllByIsActiveTrue();
}
