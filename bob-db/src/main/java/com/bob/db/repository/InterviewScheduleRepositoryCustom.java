package com.bob.db.repository;

import com.bob.db.entity.InterviewScheduleEntity;
import com.bob.db.enums.InterviewSchedulingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InterviewScheduleRepositoryCustom {
    Page<InterviewScheduleEntity> findInterviewSchedulingsWithFilters(
            String searchText,
            List<InterviewSchedulingStatus> statusList,
            List<UUID> positionIds,
            Pageable pageable);
    
    InterviewScheduleEntity saveWithAudit(InterviewScheduleEntity oldEntity, InterviewScheduleEntity newEntity);
    
    List<InterviewScheduleEntity> saveAllWithAudit(List<InterviewScheduleEntity> oldEntities, List<InterviewScheduleEntity> newEntities);
}
