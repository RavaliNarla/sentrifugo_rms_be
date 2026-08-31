package com.bob.db.mapper;

import com.bob.db.dto.GenderMasterDTO;
import com.bob.db.entity.GenderMasterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GenderMasterMapper {

    GenderMasterDTO toDto(GenderMasterEntity genderMasterEntity);

    GenderMasterEntity toEntity(GenderMasterDTO genderMasterDTO);

    List<GenderMasterDTO> toDtoList(List<GenderMasterEntity> genderMasterEntityList);

    List<GenderMasterEntity> toEntityList(List<GenderMasterDTO> genderMasterDTOList);

    void updateEntityFromDto(GenderMasterDTO genderMasterDTO, @MappingTarget GenderMasterEntity genderMasterEntity);
}
