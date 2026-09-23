package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CertificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificationRepository extends JpaRepository<CertificationEntity, UUID> {
    List<CertificationEntity> findAllByOrderByNameAsc();

    List<CertificationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    // Paginated variants for the admin list screen (the unpaginated methods above stay as-is
    // since they're also used to populate the Add Position Certifications dropdown).
    Page<CertificationEntity> findAllByOrderByNameAsc(Pageable pageable);
    Page<CertificationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    /** Includes soft-deleted rows so seeders do not violate the unique name constraint. */
    @Query(value = "SELECT COUNT(*) > 0 FROM common.certifications WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    boolean existsByNameIgnoreCaseIncludingInactive(@Param("name") String name);
}
