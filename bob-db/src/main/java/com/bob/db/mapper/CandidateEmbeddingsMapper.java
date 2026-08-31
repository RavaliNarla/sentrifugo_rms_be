package com.bob.db.mapper;

import com.bob.db.dto.CandidateEmbeddingsDTO;
import com.bob.db.entity.CandidateEmbeddingsEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CandidateEmbeddingsMapper {

    CandidateEmbeddingsDTO toDTO(CandidateEmbeddingsEntity entity);

    CandidateEmbeddingsEntity toEntity(CandidateEmbeddingsDTO dto);

    List<CandidateEmbeddingsDTO> toDTOList(List<CandidateEmbeddingsEntity> entities);

    List<CandidateEmbeddingsEntity> toEntityList(List<CandidateEmbeddingsDTO> dtos);

    void updateEntityFromDto(CandidateEmbeddingsDTO dto, @MappingTarget CandidateEmbeddingsEntity entity);


}

