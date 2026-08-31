package com.bob.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bob.db.entity.LanguagesKnownEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface LanguagesKnownRepository extends JpaRepository<LanguagesKnownEntity, UUID> {
    List<LanguagesKnownEntity> findByCandidateId(UUID candidateId);

    Optional<LanguagesKnownEntity> findByCandidateIdAndLanguageId(UUID candidateId, UUID languageId);

//    Optional<List<LanguagesKnownEntity>> findByAllCandidateId(UUID candidateId);
}
