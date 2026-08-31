package com.bob.db.mapper;

import com.bob.db.dto.MedicalCentresDTO;
import com.bob.db.entity.MedicalCentresEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MedicalCentresMapper {
    MedicalCentresDTO toDto(MedicalCentresEntity entity);
    MedicalCentresEntity toEntity(MedicalCentresDTO dto);
    List<MedicalCentresDTO> toDtoList(List<MedicalCentresEntity> entities);
    List<MedicalCentresEntity> toEntityList(List<MedicalCentresDTO> dto);
}
