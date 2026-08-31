package com.bob.db.repository;

import com.bob.db.entity.InterviewPanelScheduleConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface InterviewPanelScheduleConfigurationRepository extends JpaRepository<InterviewPanelScheduleConfigurationEntity, UUID> {


    List<InterviewPanelScheduleConfigurationEntity> findByApplicationIdIn(Collection<UUID> applicationIds);
}