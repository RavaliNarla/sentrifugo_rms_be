package com.bob.db.mapper;

import com.bob.db.dto.JobPositionExclusionsDTO;
import com.bob.db.entity.JobPositionExclusionsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobPositionExclusionsMapper {

    @Mapping(target = "positionId", source = "jobPosition.id")
    JobPositionExclusionsDTO toDto(JobPositionExclusionsEntity entity);

    @Mapping(target = "jobPosition.id", source = "positionId")
    JobPositionExclusionsEntity toEntity(JobPositionExclusionsDTO dto);

    List<JobPositionExclusionsDTO> toDtoList(List<JobPositionExclusionsEntity> entities);

    List<JobPositionExclusionsEntity> toEntityList(List<JobPositionExclusionsDTO> dtos);
}

