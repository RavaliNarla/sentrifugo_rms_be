package com.bob.db.mapper;

import com.bob.db.dto.ReligionMasterDTO;
import com.bob.db.entity.ReligionMasterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReligionMasterMapper {

    ReligionMasterEntity toEntity(ReligionMasterDTO religionMasterDTO);
    ReligionMasterDTO toDTO(ReligionMasterEntity religionMasterEntity);
    List<ReligionMasterEntity> toEntityList(List<ReligionMasterDTO> religionMasterDTOS);
    List<ReligionMasterDTO> toDTOList(List<ReligionMasterEntity> religionMasterEntities);
    void updateEntityFromDTO(ReligionMasterDTO religionMasterDTO,@MappingTarget ReligionMasterEntity religionMasterEntity);
}
