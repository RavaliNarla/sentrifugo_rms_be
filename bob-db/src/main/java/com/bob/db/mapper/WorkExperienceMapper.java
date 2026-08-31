package com.bob.db.mapper;

import com.bob.db.dto.WorkExperienceDTO;
import com.bob.db.entity.WorkExperienceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface WorkExperienceMapper {

    WorkExperienceEntity toEntity(WorkExperienceDTO workExperienceDTO);

    WorkExperienceDTO toDTO(WorkExperienceEntity workExperienceEntity);

    List<WorkExperienceDTO> toDTOList(List<WorkExperienceEntity> workExperienceEntityList);

    List<WorkExperienceEntity> toEntityList(List<WorkExperienceDTO> workExperienceDTOList);

    void updateEntityFromDto(WorkExperienceDTO workExperienceDTO,
                             @MappingTarget WorkExperienceEntity workExperienceEntity);
}
