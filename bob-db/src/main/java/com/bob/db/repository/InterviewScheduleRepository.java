package com.bob.db.repository;


import com.bob.db.entity.InterviewScheduleEntity;
import com.bob.db.enums.InterviewSchedulingStatus;
import com.bob.db.enums.MailSendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface InterviewScheduleRepository
        extends JpaRepository<InterviewScheduleEntity, UUID>,InterviewScheduleRepositoryCustom,JpaSpecificationExecutor<InterviewScheduleEntity> {

    @Query("SELECT i FROM InterviewScheduleEntity i WHERE i.panelId IN :panelIds " +
            "AND i.interviewStartAt >= :start AND i.interviewStartAt < :end")
    List<InterviewScheduleEntity> findPanelSchedulings(
            @Param("panelIds") Collection<UUID> panelIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT i FROM InterviewScheduleEntity i WHERE " +
            "CAST(i.interviewStartAt AS date) = :interviewDate " +
            "AND i.zonalOfficeId = :zonalOfficeId " +
            "AND i.interviewStatus IN :statuses")
    List<InterviewScheduleEntity> findByInterviewDateAndZonalOfficeIdAndStatuses(
            @Param("interviewDate") LocalDate interviewDate,
            @Param("zonalOfficeId") UUID zonalOfficeId,
            @Param("statuses") List<InterviewSchedulingStatus> statuses
    );

    List<InterviewScheduleEntity> findByInterviewStatusAndMailSendStatus(
            InterviewSchedulingStatus interviewStatus,
            MailSendStatus mailSendStatus);

    List<InterviewScheduleEntity> findByInterviewStatusAndMailSendStatusAndCreatedDateBetween(
            InterviewSchedulingStatus interviewStatus,
            MailSendStatus mailSendStatus,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<InterviewScheduleEntity> findByPanelId(UUID panelId);

    Optional<InterviewScheduleEntity> findByApplicationId(UUID applicationId);
    List<InterviewScheduleEntity> findByApplicationIdIn(List<UUID> applicationIds);
    @Query("""
    SELECT i.panelId, FUNCTION('DATE', i.interviewStartAt), i.meetingLink
    FROM InterviewScheduleEntity i
    WHERE i.panelId IN :panelIds
      AND i.interviewStartAt BETWEEN :start AND :end
      AND i.meetingLink IS NOT NULL
""")
    List<Object[]> findMeetingLinksBulk(Set<UUID> panelIds,
                                        LocalDateTime start,
                                        LocalDateTime end);
    @Query("SELECT s FROM InterviewScheduleEntity s WHERE " +
            "s.interviewStartAt < :endTime AND s.interviewEndAt > :startTime")
    List<InterviewScheduleEntity> findExistingSchedulesInRange(
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT s
        FROM InterviewScheduleEntity s
        WHERE
        s.interviewStartAt < :maxEnd
        AND s.interviewEndAt > :minStart
        AND s.applicationId NOT IN :applicationIds
""")
    List<InterviewScheduleEntity> findExistingSchedulesInRangeAndApplicationIdNotIn(
            @Param("minStart") LocalDateTime minStart,
            @Param("maxEnd") LocalDateTime maxEnd,
            @Param("applicationIds") List<UUID> applicationIds
    );

    List<InterviewScheduleEntity> findAllByApplicationIdIn(List<UUID> applicationIds);
}
