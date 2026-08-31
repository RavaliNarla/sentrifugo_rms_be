package com.bob.db.repository;

import com.bob.db.entity.CandidatesEntity;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidatesRepository extends JpaRepository<CandidatesEntity, UUID> {
    Optional<CandidatesEntity> findByEmailIgnoreCase(String email);

    Optional<CandidatesEntity> findByRefreshToken(String refreshToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobileNumber(String mobileNumber);

    List<CandidatesEntity> findAllByIdIn(List<UUID> candidateIds);
}
