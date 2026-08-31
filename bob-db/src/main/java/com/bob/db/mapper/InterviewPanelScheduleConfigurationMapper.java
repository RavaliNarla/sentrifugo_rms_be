package com.bob.db.mapper;

import com.bob.db.dto.InterviewPanelScheduleConfigurationDTO;
import com.bob.db.entity.InterviewPanelScheduleConfigurationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface InterviewPanelScheduleConfigurationMapper {

    InterviewPanelScheduleConfigurationDTO toDto(InterviewPanelScheduleConfigurationEntity entity);

    InterviewPanelScheduleConfigurationEntity toEntity(InterviewPanelScheduleConfigurationDTO dto);

    List<InterviewPanelScheduleConfigurationDTO> toDtoList(List<InterviewPanelScheduleConfigurationEntity> entityList);

    List<InterviewPanelScheduleConfigurationEntity> toEntityList(List<InterviewPanelScheduleConfigurationDTO> dtoList);

    void updateEntityFromDto(InterviewPanelScheduleConfigurationDTO dto, @MappingTarget InterviewPanelScheduleConfigurationEntity entity);

}