package com.bob.db.repository;

import com.bob.db.entity.InterviewPanelMembersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface InterviewPanelMembersRepository extends JpaRepository<InterviewPanelMembersEntity, UUID> {
    List<InterviewPanelMembersEntity> findAllByPanelId(UUID id);

    // JPQL to eagerly fetch the panelMember association to avoid LazyInitializationException
    @Query("select m from InterviewPanelMembersEntity m join fetch m.panelMember where m.panel.id = :panelId")
    List<InterviewPanelMembersEntity> findAllWithPanelMemberByPanelId(@Param("panelId") UUID panelId);

    List<InterviewPanelMembersEntity> findAllByPanel_IdIn(Collection<UUID> panelIds);


    List<InterviewPanelMembersEntity> findAllByPanelMember_Id(UUID memberId);
}
