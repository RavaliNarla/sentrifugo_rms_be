package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.EducationQualificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EducationQualificationRepository extends JpaRepository<EducationQualificationEntity, UUID> {
    List<EducationQualificationEntity> findAllByOrderByNameAsc();
}
