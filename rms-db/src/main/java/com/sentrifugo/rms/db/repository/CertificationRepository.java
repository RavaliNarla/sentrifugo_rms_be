package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificationRepository extends JpaRepository<CertificationEntity, UUID> {
    List<CertificationEntity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
