package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionTitleRepository extends JpaRepository<PositionTitleEntity, UUID> {
    List<PositionTitleEntity> findAllByDepartmentIdOrderByNameAsc(UUID departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, UUID departmentId);

    // Unpaginated - used by MasterDataSeeder to look up existing rows by name.
    List<PositionTitleEntity> findAllByOrderByNameAsc();

    // Paginated - drives the Position Titles admin screen.
    Page<PositionTitleEntity> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
        SELECT p FROM PositionTitleEntity p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(p.jobDescription) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY p.name ASC
        """)
    Page<PositionTitleEntity> search(@Param("search") String search, Pageable pageable);
}
