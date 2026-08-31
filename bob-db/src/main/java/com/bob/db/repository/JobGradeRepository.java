package com.bob.db.repository;

import com.bob.db.entity.JobGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobGradeRepository extends JpaRepository<JobGradeEntity, UUID> {


    List<JobGradeEntity> findAllByIsActiveTrue();


}
