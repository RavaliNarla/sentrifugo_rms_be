package com.bob.db.mapper;

import com.bob.db.dto.JobRequisitionsDTO;
import com.bob.db.entity.JobRequisitionsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobRequisitionMapper {
    JobRequisitionsEntity toEntity(JobRequisitionsDTO dto);
    JobRequisitionsDTO toDTO(JobRequisitionsEntity entity);
    List<JobRequisitionsDTO> toDTOs(List<JobRequisitionsEntity> entities);

    List<JobRequisitionsEntity> toEntities(List<JobRequisitionsDTO> dtos);
    void updateEntityFromDto(JobRequisitionsDTO dto,@MappingTarget JobRequisitionsEntity entity);
}
