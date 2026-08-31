package com.bob.db.repository;

import com.bob.db.entity.CandidateCertificationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateCertificationsRepository extends JpaRepository<CandidateCertificationsEntity, UUID> {

    List<CandidateCertificationsEntity> findByCandidateId(UUID candidateId);
}

