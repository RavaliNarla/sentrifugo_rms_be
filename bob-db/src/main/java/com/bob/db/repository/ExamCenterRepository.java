package com.bob.db.repository;

import com.bob.db.entity.ExamCenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamCenterRepository extends JpaRepository<ExamCenterEntity, UUID> {
}
