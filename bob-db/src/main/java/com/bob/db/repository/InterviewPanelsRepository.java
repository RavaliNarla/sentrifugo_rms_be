package com.bob.db.repository;

import com.bob.db.entity.InterviewPanelsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewPanelsRepository extends JpaRepository<InterviewPanelsEntity, UUID>, JpaSpecificationExecutor<InterviewPanelsEntity> {

    Optional<InterviewPanelsEntity> findByPanelNameAndCommittee_Id(String panelName, UUID committeeId);

    Optional<InterviewPanelsEntity> findByPanelNameAndCommittee_IdAndIdNot(String panelName, UUID committeeId, UUID id);

    @Query("SELECT ip FROM InterviewPanelsEntity ip LEFT JOIN FETCH ip.panelMembers LEFT JOIN FETCH ip.committee WHERE ip.id = :id")
    Optional<InterviewPanelsEntity> findByIdWithPanelMembers(UUID id);

    List<InterviewPanelsEntity> findByCommittee_Id(UUID id);
    @Query("""
    select distinct p
    from InterviewPanelsEntity p
    left join fetch p.panelMembers pm
    left join fetch pm.panelMember
    where p.id in :panelIds
""")
    List<InterviewPanelsEntity> findAllWithMembers(
            @Param("panelIds") List<UUID> panelIds
    );
}
