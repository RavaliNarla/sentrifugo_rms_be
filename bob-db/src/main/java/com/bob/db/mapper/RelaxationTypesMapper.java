package com.bob.db.mapper;

import com.bob.db.dto.RelaxationTypesDTO;
import com.bob.db.entity.RelaxationTypesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RelaxationTypesMapper {
    RelaxationTypesDTO toDTO(RelaxationTypesEntity relaxationTypeEntity);
    RelaxationTypesEntity toEntity(RelaxationTypesDTO relaxationTypeDTO);
    List<RelaxationTypesDTO> toDTOList(List<RelaxationTypesEntity> relaxationTypeEntities);
    List<RelaxationTypesEntity> toEntityList(List<RelaxationTypesDTO> relaxationTypeDTOS);
    void updateEntityFromDto(RelaxationTypesDTO relaxationTypeDTO,@MappingTarget RelaxationTypesEntity relaxationTypeEntity);
}
