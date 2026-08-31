package com.bob.db.repository;

import com.bob.db.entity.CandidateApplicationDocumentVerificationEntity;
import com.bob.db.enums.DocumentScreeningStatus;
import com.bob.db.enums.DocumentZonalVerificationStatus;
import com.bob.db.enums.ZonalVerificationStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateApplicationDocumentVerificationRepository extends JpaRepository<CandidateApplicationDocumentVerificationEntity, UUID> {

    Optional<CandidateApplicationDocumentVerificationEntity> findByCandidateDocumentId(UUID candidateDocumentId);

    List<CandidateApplicationDocumentVerificationEntity> findByApplicationId(UUID applicationId);

    List<CandidateApplicationDocumentVerificationEntity> findByCandidateId(UUID candidateId);

    Optional<CandidateApplicationDocumentVerificationEntity> findByApplicationIdAndCandidateDocumentId( UUID applicationId, UUID candidateDocumentId);

//    @Query("""
//        SELECT d
//        FROM CandidateApplicationDocumentVerificationEntity d
//        WHERE d.applicationId = :applicationId
//          AND (
//               d.docScreeningStatus = :screeningStatus
//            OR d.zonalHrDocStatus = :zonalStatus
//          )
//    """)
//    List<CandidateApplicationDocumentVerificationEntity> findPendingDocumentsByApplicationId(
//            @Param("applicationId") UUID applicationId,
//            @Param("screeningStatus") DocumentScreeningStatus screeningStatus,
//            @Param("zonalStatus") DocumentZonalVerificationStatus zonalStatus
//    );

    List<CandidateApplicationDocumentVerificationEntity> findByApplicationIdAndDocScreeningStatus(UUID applicationId, DocumentScreeningStatus documentScreeningStatus);

    List<CandidateApplicationDocumentVerificationEntity> findByCandidateDocumentIdAndDocScreeningStatus(UUID candidateDocumentId, DocumentScreeningStatus documentScreeningStatus);

    List<CandidateApplicationDocumentVerificationEntity> findAllByApplicationIdInAndDocScreeningStatus(Iterable<UUID> listOfApplication,DocumentScreeningStatus documentScreeningStatus);

    List<CandidateApplicationDocumentVerificationEntity> findByApplicationIdAndZonalHrDocStatus(UUID applicationId, DocumentZonalVerificationStatus documentZonalVerificationStatus);

    List<CandidateApplicationDocumentVerificationEntity> findByCandidateDocumentIdAndZonalHrDocStatus(UUID candidateDocumentId, DocumentZonalVerificationStatus documentScreeningStatus);

    List<CandidateApplicationDocumentVerificationEntity> findAllByApplicationIdInAndZonalHrDocStatus(List<UUID> listOfApplicationwithoutfilter, DocumentZonalVerificationStatus documentScreeningStatus);

    @Query("""
        SELECT c 
        FROM CandidateApplicationDocumentVerificationEntity c
        WHERE c.applicationId IN :listOfApplication
        AND (
            c.docScreeningStatus = :docScreeningStatus
            OR c.zonalHrDocStatus = :documentZonalVerificationStatus
            )
        """)
    List<CandidateApplicationDocumentVerificationEntity> findAllByApplicationIdInAndDocScreeningStatusOrZonalHrDocStatus(@Param("listOfApplication") Iterable<UUID> listOfApplication,@Param("docScreeningStatus") DocumentScreeningStatus documentScreeningStatus,@Param("documentZonalVerificationStatus") DocumentZonalVerificationStatus documentZonalVerificationStatus);

    List<CandidateApplicationDocumentVerificationEntity> findByApplicationIdOrderByDisplayNameAsc(UUID applicationId);

    Optional<CandidateApplicationDocumentVerificationEntity> findByCandidateIdAndDocumentIdAndApplicationId(UUID candidateId, UUID DocumentId,UUID applicationId);
}

