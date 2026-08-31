package com.bob.db.repository;

import com.bob.db.entity.RequestTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface RequestTypesRepository extends JpaRepository<RequestTypesEntity, UUID> {
    Optional<RequestTypesEntity> findByRequestName(String otherRequest);
}

