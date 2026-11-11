package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.LocationEntity;
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
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    List<LocationEntity> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(l.address, '')) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(l.code, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY l.name ASC
        """)
    List<LocationEntity> search(@Param("search") String search);

    Page<LocationEntity> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
        SELECT l FROM LocationEntity l
        WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(l.address, '')) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(l.code, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY l.name ASC
        """)
    Page<LocationEntity> search(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT * FROM common.locations WHERE hrms_id = :hrmsId LIMIT 1", nativeQuery = true)
    Optional<LocationEntity> findIncludingInactiveByHrmsId(@Param("hrmsId") String hrmsId);

    @Query(value = "SELECT * FROM common.locations WHERE hrms_id IS NOT NULL", nativeQuery = true)
    List<LocationEntity> findAllWithHrmsIdIncludingInactive();
}
