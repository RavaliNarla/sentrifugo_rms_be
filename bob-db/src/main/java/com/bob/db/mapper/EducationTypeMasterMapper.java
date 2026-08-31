package com.bob.db.mapper;


import com.bob.db.dto.EducationTypeMasterDTO;
import com.bob.db.entity.EducationTypeMasterEntity;
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
public interface EducationTypeMasterMapper {

    EducationTypeMasterEntity toEntity(EducationTypeMasterDTO dto);

    EducationTypeMasterDTO toDTO(EducationTypeMasterEntity entity);

    List<EducationTypeMasterDTO> toDTOList(List<EducationTypeMasterEntity> entityList);

    List<EducationTypeMasterEntity> toEntityList(List<EducationTypeMasterDTO> dtoList);

    void updateEntityFromDto(EducationTypeMasterDTO dto,
                             @MappingTarget EducationTypeMasterEntity entity);
}
