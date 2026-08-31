package com.bob.db.mapper;

import com.bob.db.dto.JobSelectionProcessDTO;
import com.bob.db.entity.JobSelectionProcessEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobSelectionProcessMapper {
    JobSelectionProcessDTO toDto(JobSelectionProcessEntity entity);

    JobSelectionProcessEntity toEntity(JobSelectionProcessDTO dto);

    List<JobSelectionProcessDTO> toDTOList(List<JobSelectionProcessEntity> entities);

    List<JobSelectionProcessEntity> toEntityList(List<JobSelectionProcessDTO> dtos);
}
