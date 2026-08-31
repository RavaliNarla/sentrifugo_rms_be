package com.bob.db.mapper;

import com.bob.db.dto.CandidatesDTO;
import com.bob.db.entity.CandidatesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CandidatesMapper {
    CandidatesMapper INSTANCE = Mappers.getMapper(CandidatesMapper.class);

    CandidatesDTO toDto(CandidatesEntity entity);

    CandidatesEntity toEntity(CandidatesDTO dto);

    List<CandidatesDTO> toDtoList(List<CandidatesEntity> entities);

    List<CandidatesEntity> toEntityList(List<CandidatesDTO> dtos);
}
