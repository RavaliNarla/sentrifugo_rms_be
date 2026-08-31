package com.bob.db.mapper;


import com.bob.db.dto.UniversityMasterDTO;
import com.bob.db.entity.UniversityMasterEntity;
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
public interface UniversityMasterMapper {

    UniversityMasterEntity toEntity(UniversityMasterDTO dto);

    UniversityMasterDTO toDTO(UniversityMasterEntity entity);

    List<UniversityMasterDTO> toDTOList(List<UniversityMasterEntity> entityList);

    List<UniversityMasterEntity> toEntityList(List<UniversityMasterDTO> dtoList);

    void updateEntityFromDto(UniversityMasterDTO dto, @MappingTarget UniversityMasterEntity entity);
}
