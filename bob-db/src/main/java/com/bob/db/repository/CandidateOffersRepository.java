package com.bob.db.repository;

import com.bob.db.entity.CandidateOffersEntity;
import com.bob.db.enums.CandidateOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CandidateOffersRepository extends JpaRepository<CandidateOffersEntity, UUID> {
    List<CandidateOffersEntity> findByJobPosition_Id(UUID positionId);
    Optional<CandidateOffersEntity> findByCandidateApplication_Id(UUID applicationId);
    List<CandidateOffersEntity> findAllByCandidateApplication_IdIn(List<UUID> applicationIds);
    List<CandidateOffersEntity> findAllByJobPosition_IdAndStatusIn(UUID positionId, List<CandidateOfferStatus> status);
    List<CandidateOffersEntity> findAllByCandidateApplication_IdInAndStatusIn(List<UUID> applicationIds, List<CandidateOfferStatus> status);


    List<CandidateOffersEntity> findByJobPosition_IdAndOfferFileUrlIn(UUID positionId,List<String> fileUrls);

    @Query("""
    SELECT co.letterNumber
    FROM CandidateOffersEntity co
    WHERE co.letterNumber IS NOT NULL
    AND co.id NOT IN :excludedOfferIds
    """)
    Set<String> findOfferLetterNumbers(@Param("excludedOfferIds") Iterable<UUID> excludedOfferIds);
}
