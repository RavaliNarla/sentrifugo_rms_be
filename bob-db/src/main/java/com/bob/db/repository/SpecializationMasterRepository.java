package com.bob.db.repository;


import com.bob.db.entity.SpecializationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface SpecializationMasterRepository extends JpaRepository<SpecializationMasterEntity, UUID> {

    List<SpecializationMasterEntity>  findAllByEducationQualificationsId(UUID educationQualificationsId);
    List<SpecializationMasterEntity> findAllByEducationQualificationsIdIn(List<UUID> educationQualificationIds);

}
