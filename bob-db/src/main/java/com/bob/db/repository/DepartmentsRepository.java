package com.bob.db.repository;

import com.bob.db.entity.DepartmentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentsRepository extends JpaRepository<DepartmentsEntity, UUID> {


    List<DepartmentsEntity> findAllByIsActiveTrue();
}
