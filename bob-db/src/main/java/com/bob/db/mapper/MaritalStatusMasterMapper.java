package com.bob.db.mapper;

import com.bob.db.dto.MaritalStatusMasterDTO;
import com.bob.db.entity.MaritalStatusMasterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MaritalStatusMasterMapper {
    MaritalStatusMasterDTO toDto(MaritalStatusMasterEntity maritalStatusMasterEntity);
    MaritalStatusMasterEntity toEntity(MaritalStatusMasterDTO maritalStatusMasterDTO);
    List<MaritalStatusMasterEntity> toEntityList(List<MaritalStatusMasterDTO> maritalStatusMasterDTOS);
    List<MaritalStatusMasterDTO> toDtoList(List<MaritalStatusMasterEntity> maritalStatusMasterEntities);

}
