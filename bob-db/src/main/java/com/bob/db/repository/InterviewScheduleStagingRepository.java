package com.bob.db.repository;

import com.bob.db.entity.CandidateCompensationEntity;
import com.bob.db.entity.InterviewScheduleEntity;
import com.bob.db.entity.InterviewScheduleStagingEntity;
import com.bob.db.enums.InterviewSchedulingApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface InterviewScheduleStagingRepository extends JpaRepository<InterviewScheduleStagingEntity, UUID>,JpaSpecificationExecutor<InterviewScheduleStagingEntity> {

    @Query("""
    SELECT s
    FROM InterviewScheduleStagingEntity s
    WHERE
        s.interviewStartAt < :maxEnd
        AND s.interviewEndAt > :minStart
        AND s.application.id NOT IN :applicationIds
        AND s.interviewSchedulingApprovalStatus IN :statusList
""")
    List<InterviewScheduleStagingEntity> findExistingSchedulesInRangeAndApplicationIdNotIn(
            @Param("minStart") LocalDateTime minStart,
            @Param("maxEnd") LocalDateTime maxEnd,
            @Param("applicationIds") List<UUID> applicationIds,
            @Param("statusList") List<InterviewSchedulingApprovalStatus> statusList
    );

    List<InterviewScheduleStagingEntity> findByApplicationIdInAndInterviewSchedulingApprovalStatusIn(List<UUID> applicationIds, List<InterviewSchedulingApprovalStatus> statusList);

    List<InterviewScheduleStagingEntity> findByApplicationIdIn(List<UUID> applicationIds);
}
