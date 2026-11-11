package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.DepartmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findAllByOrderByNameAsc();
    List<DepartmentEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Page<DepartmentEntity> findAllByOrderByNameAsc(Pageable pageable);
    Page<DepartmentEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    @Query(value = "SELECT * FROM common.departments WHERE hrms_id = :hrmsId LIMIT 1", nativeQuery = true)
    Optional<DepartmentEntity> findIncludingInactiveByHrmsId(@Param("hrmsId") String hrmsId);

    @Query(value = "SELECT * FROM common.departments WHERE hrms_id IS NOT NULL", nativeQuery = true)
    List<DepartmentEntity> findAllWithHrmsIdIncludingInactive();
}
