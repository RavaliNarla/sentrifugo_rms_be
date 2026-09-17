package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificationRepository extends JpaRepository<CertificationEntity, UUID> {
    List<CertificationEntity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    /** Includes soft-deleted rows so seeders do not violate the unique name constraint. */
    @Query(value = "SELECT COUNT(*) > 0 FROM common.certifications WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    boolean existsByNameIgnoreCaseIncludingInactive(@Param("name") String name);
}
