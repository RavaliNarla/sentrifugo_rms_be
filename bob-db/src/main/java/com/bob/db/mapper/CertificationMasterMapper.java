package com.bob.db.mapper;

import com.bob.db.dto.CertificationMasterDTO;
import com.bob.db.entity.CertificationMasterEntity;
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
public interface CertificationMasterMapper {
    CertificationMasterEntity toEntity(CertificationMasterDTO certificationMasterDTO);
    CertificationMasterDTO toDTO(CertificationMasterEntity certificationMasterEntity);
    List<CertificationMasterEntity> toEntityList(List<CertificationMasterDTO> certificationMasterDTOS);
    List<CertificationMasterDTO> toDTOList(List<CertificationMasterEntity> certificationMasterEntity);
    void updateEntityFromDTO(CertificationMasterDTO certificationMasterDTO, @MappingTarget CertificationMasterEntity certificationMasterEntity);
}
