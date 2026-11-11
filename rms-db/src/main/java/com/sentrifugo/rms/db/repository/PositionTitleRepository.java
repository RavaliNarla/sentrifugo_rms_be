package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.PositionTitleEntity;
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
public interface PositionTitleRepository extends JpaRepository<PositionTitleEntity, UUID> {

    /** Titles linked to this department, or unlinked (appear under every department). */
    @Query("""
        SELECT p FROM PositionTitleEntity p
        WHERE p.departmentId IS NULL OR p.departmentId = :departmentId
        ORDER BY p.name ASC
        """)
    List<PositionTitleEntity> findAvailableForDepartment(@Param("departmentId") UUID departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, UUID departmentId);

    List<PositionTitleEntity> findAllByOrderByNameAsc();

    Page<PositionTitleEntity> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
        SELECT p FROM PositionTitleEntity p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(p.jobDescription, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY p.name ASC
        """)
    Page<PositionTitleEntity> search(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT * FROM common.position_titles WHERE hrms_id = :hrmsId LIMIT 1", nativeQuery = true)
    Optional<PositionTitleEntity> findIncludingInactiveByHrmsId(@Param("hrmsId") String hrmsId);

    @Query(value = "SELECT * FROM common.position_titles WHERE hrms_id IS NOT NULL", nativeQuery = true)
    List<PositionTitleEntity> findAllWithHrmsIdIncludingInactive();
}
