package com.bob.db.mapper;

import com.bob.db.dto.CandidateRankingResultsDTO;
import com.bob.db.entity.CandidateRankingResultsEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CandidateRankingResultsMapper {
    CandidateRankingResultsMapper INSTANCE = Mappers.getMapper(CandidateRankingResultsMapper.class);

    CandidateRankingResultsDTO toDTO(CandidateRankingResultsEntity entity);

    CandidateRankingResultsEntity toEntity(CandidateRankingResultsDTO dto);

    List<CandidateRankingResultsDTO> toDTOList(List<CandidateRankingResultsEntity> entities);

    List<CandidateRankingResultsEntity> toEntityList(List<CandidateRankingResultsDTO> dtos);

    void updateEntityFromDto(CandidateRankingResultsDTO dto, @MappingTarget CandidateRankingResultsEntity entity);
}

