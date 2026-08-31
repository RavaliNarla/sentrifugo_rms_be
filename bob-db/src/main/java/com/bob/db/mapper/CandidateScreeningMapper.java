package com.bob.db.mapper;

import com.bob.db.dto.CandidateScreeningDTO;
import com.bob.db.entity.CandidateScreeningEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateScreeningMapper {

    CandidateScreeningDTO toDto(CandidateScreeningEntity entity);

    List<CandidateScreeningDTO> toDtoList(List<CandidateScreeningEntity> entities);

    CandidateScreeningEntity toEntity(CandidateScreeningDTO dto);

    List<CandidateScreeningEntity> toEntityList(List<CandidateScreeningDTO> dtos);

    void updateEntityFromDto(CandidateScreeningDTO dto, @MappingTarget CandidateScreeningEntity entity);

}


