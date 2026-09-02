package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
