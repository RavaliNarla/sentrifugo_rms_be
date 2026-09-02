package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
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

    List<CandidateEntity> findByIdIn(List<UUID> ids);

    long countByPositionIdAndStatus(UUID positionId, CandidateStatus status);
}
