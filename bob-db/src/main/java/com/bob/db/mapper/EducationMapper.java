package com.bob.db.mapper;

import com.bob.db.dto.EducationDTO;
import com.bob.db.entity.EducationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface EducationMapper {

    EducationEntity toEntity(EducationDTO educationDTO);

    EducationDTO toDTO(EducationEntity educationEntity);

    List<EducationDTO> toDTOList(List<EducationEntity> educationEntityList);

    List<EducationEntity> toEntityList(List<EducationDTO> educationDTOList);

    void updateEntityFromDto(EducationDTO educationDTO,
                             @MappingTarget EducationEntity educationEntity);
}
