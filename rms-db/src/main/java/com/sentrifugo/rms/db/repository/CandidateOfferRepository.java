package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CandidateOfferEntity;
import com.sentrifugo.rms.db.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateOfferRepository extends JpaRepository<CandidateOfferEntity, UUID> {
    Optional<CandidateOfferEntity> findByCandidateId(UUID candidateId);
    Optional<CandidateOfferEntity> findByAcceptToken(UUID acceptToken);
    List<CandidateOfferEntity> findByCandidateIdIn(List<UUID> candidateIds);
    List<CandidateOfferEntity> findByStatus(OfferStatus status);
    List<CandidateOfferEntity> findByStatusIn(List<OfferStatus> statuses);

    @Query("SELECT o FROM CandidateOfferEntity o WHERE o.supersededTokens IS NOT NULL AND o.supersededTokens LIKE CONCAT('%', :token, '%')")
    Optional<CandidateOfferEntity> findBySupersededTokenContaining(@Param("token") String token);

    // Drives the Offer Approvals screen: server-side candidate-name search + status-filter, paginated,
    // newest first. allowedStatuses is the fixed set of statuses visible at that approval level; status
    // further narrows within that set when the approver picks a specific status filter.
    @Query("SELECT o FROM CandidateOfferEntity o WHERE o.status IN :allowedStatuses AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR EXISTS (SELECT 1 FROM CandidateEntity c WHERE c.id = o.candidateId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<CandidateOfferEntity> searchForApproval(
            @Param("allowedStatuses") List<OfferStatus> allowedStatuses,
            @Param("status") OfferStatus status,
            @Param("search") String search,
            Pageable pageable);
}
