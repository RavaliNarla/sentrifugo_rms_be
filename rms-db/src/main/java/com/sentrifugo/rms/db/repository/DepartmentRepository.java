package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.DepartmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findAllByOrderByNameAsc();
    List<DepartmentEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    boolean existsByNameIgnoreCase(String name);

    // Paginated variants for the admin list screen (the unpaginated methods above stay as-is
    // since they're also used to populate dropdowns elsewhere, which need the full list).
    Page<DepartmentEntity> findAllByOrderByNameAsc(Pageable pageable);
    Page<DepartmentEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
