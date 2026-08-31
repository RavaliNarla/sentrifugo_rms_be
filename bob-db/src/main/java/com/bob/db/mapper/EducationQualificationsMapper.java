package com.bob.db.mapper;

import com.bob.db.dto.EducationQualificationsDTO;
import com.bob.db.entity.EducationQualificationsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EducationQualificationsMapper {

    EducationQualificationsEntity toEntity(EducationQualificationsDTO dto);

    EducationQualificationsDTO toDTO(EducationQualificationsEntity entity);

    List<EducationQualificationsDTO> toDTOList(List<EducationQualificationsEntity> entityList);

    List<EducationQualificationsEntity> toEntityList(List<EducationQualificationsDTO> dtoList);

    void updateEntityFromDto(EducationQualificationsDTO dto,
                             @MappingTarget EducationQualificationsEntity entity);
}

