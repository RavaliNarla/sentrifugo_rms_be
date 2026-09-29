package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateEntity, UUID> {

    @Query("""
        SELECT c FROM CandidateEntity c
        WHERE c.positionId = :positionId
          AND (:statuses IS NULL OR c.status IN :statuses)
          AND (:searchText IS NULL OR :searchText = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')))
        ORDER BY c.createdDate DESC
        """)
    Page<CandidateEntity> search(@Param("positionId") UUID positionId,
                                  @Param("statuses") List<CandidateStatus> statuses,
                                  @Param("searchText") String searchText,
                                  Pageable pageable);

    /** Interview Pool level filter (e.g. "L2 SCHEDULED"): same as search, limited to one interview round. */
    @Query("""
        SELECT c FROM CandidateEntity c
        WHERE c.positionId = :positionId
          AND (:statuses IS NULL OR c.status IN :statuses)
          AND (:searchText IS NULL OR :searchText = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')))
          AND EXISTS (SELECT 1 FROM InterviewScheduleEntity s WHERE s.candidateId = c.id AND s.round = :round)
        ORDER BY c.createdDate DESC
        """)
    Page<CandidateEntity> searchByRound(@Param("positionId") UUID positionId,
                                        @Param("statuses") List<CandidateStatus> statuses,
                                        @Param("searchText") String searchText,
                                        @Param("round") Integer round,
                                        Pageable pageable);

    // Offer Pool status filter: candidates whose offer is in a given status...
    @Query("""
        SELECT c FROM CandidateEntity c
        WHERE c.positionId = :positionId
          AND (:statuses IS NULL OR c.status IN :statuses)
          AND (:searchText IS NULL OR :searchText = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')))
          AND EXISTS (SELECT 1 FROM CandidateOfferEntity o WHERE o.candidateId = c.id AND o.status = :offerStatus)
        ORDER BY c.createdDate DESC
        """)
    Page<CandidateEntity> searchByOfferStatus(@Param("positionId") UUID positionId,
                                              @Param("statuses") List<CandidateStatus> statuses,
                                              @Param("searchText") String searchText,
                                              @Param("offerStatus") OfferStatus offerStatus,
                                              Pageable pageable);

    // ...or candidates with no offer generated yet.
    @Query("""
        SELECT c FROM CandidateEntity c
        WHERE c.positionId = :positionId
          AND (:statuses IS NULL OR c.status IN :statuses)
          AND (:searchText IS NULL OR :searchText = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')))
          AND NOT EXISTS (SELECT 1 FROM CandidateOfferEntity o WHERE o.candidateId = c.id)
        ORDER BY c.createdDate DESC
        """)
    Page<CandidateEntity> searchWithoutOffer(@Param("positionId") UUID positionId,
                                             @Param("statuses") List<CandidateStatus> statuses,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    List<CandidateEntity> findByIdIn(List<UUID> ids);

    long countByPositionIdAndStatus(UUID positionId, CandidateStatus status);

    // Email/phone must be unique across all candidates - excludeId variants let an update
    // ignore the candidate's own current row.
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
