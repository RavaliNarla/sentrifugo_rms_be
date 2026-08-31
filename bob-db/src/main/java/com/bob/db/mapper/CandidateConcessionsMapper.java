package com.bob.db.mapper;

import com.bob.db.dto.CandidateConcessionsDTO;
import com.bob.db.entity.CandidateConcessionsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateConcessionsMapper {

    CandidateConcessionsEntity toEntity(CandidateConcessionsDTO concessionsDTO);
    CandidateConcessionsDTO toDTO(CandidateConcessionsEntity concessionsEntity);
    List<CandidateConcessionsEntity> toEntityList(List<CandidateConcessionsDTO> concessionsDTOList);
    List<CandidateConcessionsDTO> toDTOList(List<CandidateConcessionsEntity> concessionsEntityList);
    void updateEntityFromDto(CandidateConcessionsDTO dto, @MappingTarget CandidateConcessionsEntity entity);

}
