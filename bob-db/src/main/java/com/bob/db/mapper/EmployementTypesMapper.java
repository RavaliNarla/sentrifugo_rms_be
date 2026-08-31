package com.bob.db.mapper;

import com.bob.db.dto.EmployementTypesDTO;
import com.bob.db.entity.EmployementTypesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployementTypesMapper {

    EmployementTypesDTO toDto(EmployementTypesEntity employementTypesEntity);

    EmployementTypesEntity toEntity(EmployementTypesDTO employementTypesDTO);

    List<EmployementTypesDTO> toDtoList(List<EmployementTypesEntity> employementTypesEntityList);

    List<EmployementTypesEntity> toEntityList(List<EmployementTypesDTO> employementTypesDTOList);

    void updateEntityFromDto(EmployementTypesDTO employementTypesDTO, @MappingTarget EmployementTypesEntity employementTypesEntity);
}
