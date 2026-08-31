package com.bob.db.repository;

import com.bob.db.entity.CandidateRegistrationStagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRegistrationStagesRepository extends JpaRepository<CandidateRegistrationStagesEntity, UUID> {
    Optional<CandidateRegistrationStagesEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<CandidateRegistrationStagesEntity> findByVerificationToken(String token);

    void deleteAllByEmailIgnoreCase(String email);

    Boolean existsByEmailIgnoreCaseAndIsEmailVerified(String emailId, Boolean b);

    Boolean existsByMobileNumberAndIsOtpVerified(String phoneNo, Boolean isOtpVerified);

    Optional<CandidateRegistrationStagesEntity> deleteAllByEmailIgnoreCaseAndVerificationTokenNot(String email, String token);

    Optional<CandidateRegistrationStagesEntity> findByIsEmailVerifiedAndEmailIgnoreCase(boolean isEmailVerified, String email);

    Optional<CandidateRegistrationStagesEntity> findTopByEmailIgnoreCaseOrderByCreatedDateDesc(
            String email
    );
}
