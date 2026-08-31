package com.bob.db.mapper;


import com.bob.db.dto.SpecializationMasterDTO;
import com.bob.db.entity.SpecializationMasterEntity;
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
public interface SpecializationMasterMapper {

    SpecializationMasterEntity toEntity(SpecializationMasterDTO dto);

    SpecializationMasterDTO toDTO(SpecializationMasterEntity entity);

    List<SpecializationMasterDTO> toDTOList(List<SpecializationMasterEntity> entityList);

    List<SpecializationMasterEntity> toEntityList(List<SpecializationMasterDTO> dtoList);

    void updateEntityFromDto(SpecializationMasterDTO dto,
                             @MappingTarget SpecializationMasterEntity entity);
}
