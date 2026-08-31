package com.bob.db.repository;

import com.bob.db.entity.ConversationThreadsEntity;
import com.bob.db.enums.ConversationThreadsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface ConversationThreadsRepository extends JpaRepository<ConversationThreadsEntity, UUID>, JpaSpecificationExecutor<ConversationThreadsEntity> {
    List<ConversationThreadsEntity> findAllByApplicationIdIn(List<UUID> applicationId);

    List<ConversationThreadsEntity> findAllByApplicationIdInAndRequestTypeId(List<UUID> listOfApplication, UUID id);
    Page<ConversationThreadsEntity> findAllByStatusIn(List<ConversationThreadsStatus> status,Pageable pageable);


    @Query("""
    SELECT ct
    FROM ConversationThreadsEntity ct
    JOIN CandidateApplicationsEntity ca 
        ON ca.id = ct.applicationId
    JOIN CandidateProfileEntity cp 
        ON cp.candidateId = ca.candidateId
    WHERE
        (:applicationIds IS NULL OR ct.applicationId IN :applicationIds)
        AND (:requestTypeIds IS NULL OR ct.requestTypeId IN :requestTypeIds)
        AND (:statusList IS NULL OR ct.status IN :statusList)
        AND (:searchText IS NULL OR :searchText = ''
            OR LOWER(CONCAT(COALESCE(cp.firstName, ''),' ',COALESCE(cp.middleName, ''),' ',COALESCE(cp.lastName, ''))) 
            LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(ca.applicationNo, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
        )

        AND (:excludedRequestTypeIds IS NULL OR ct.requestTypeId NOT IN :excludedRequestTypeIds)
""")
    Page<ConversationThreadsEntity> searchThreads(
            @Param("applicationIds") List<UUID> applicationIds,
            @Param("requestTypeIds") List<UUID> requestTypeIds,
            @Param("statusList") List<ConversationThreadsStatus> statusList,
            @Param("searchText") String searchText,
            @Param("excludedRequestTypeIds") List<UUID> excludedRequestTypeIds,
            Pageable pageable
    );
}

