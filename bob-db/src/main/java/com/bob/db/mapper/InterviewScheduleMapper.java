package com.bob.db.mapper;


import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.entity.InterviewScheduleEntity;
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
public interface InterviewScheduleMapper {

    InterviewScheduleDTO toDto(InterviewScheduleEntity entity);

    List<InterviewScheduleDTO> toDtoList(List<InterviewScheduleEntity> entities);

    InterviewScheduleEntity toEntity(InterviewScheduleDTO dto);

    List<InterviewScheduleEntity> toEntityList(List<InterviewScheduleDTO> dtos);

    void updateEntityFromDto(InterviewScheduleDTO dto,
                             @MappingTarget InterviewScheduleEntity entity);
}
