package com.bob.db.mapper;

import com.bob.db.dto.JobGradeDTO;
import com.bob.db.entity.JobGradeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobGradeMapper {

    JobGradeDTO toDto(JobGradeEntity jobGradeEntity);

    List<JobGradeDTO> toDtoList(List<JobGradeEntity> jobGradeEntities);

    JobGradeEntity toEntity(JobGradeDTO jobGradeDTO);

    List<JobGradeEntity> toEntityList(List<JobGradeDTO> jobGradeDTOS);
    void updateEntityFromDto(JobGradeDTO jobGradeDTO,@MappingTarget JobGradeEntity jobGradeEntity);
}
