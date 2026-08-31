package com.bob.db.mapper;

import com.bob.db.dto.CandidateCertificationsDTO;
import com.bob.db.entity.CandidateCertificationsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateCertificationsMapper {

    CandidateCertificationsDTO toDto(CandidateCertificationsEntity entity);
    List<CandidateCertificationsDTO> toDtoList(List<CandidateCertificationsEntity> entities);
    CandidateCertificationsEntity toEntity(CandidateCertificationsDTO dto);
    List<CandidateCertificationsEntity> toEntityList(List<CandidateCertificationsDTO> dtos);
    void updateEntityFromDto(CandidateCertificationsDTO dto, @MappingTarget CandidateCertificationsEntity entity);

}

