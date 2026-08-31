package com.bob.db.repository;

import com.bob.db.entity.DigilockerStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigilockerStateRepository extends JpaRepository<DigilockerStateEntity, UUID> {
//    Optional<DigilockerStateEntity> findByState(String state);
    List<DigilockerStateEntity> findByState(String state);

}
