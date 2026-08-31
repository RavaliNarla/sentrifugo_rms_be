package com.bob.db.mapper;

import com.bob.db.dto.CandidateDocumentsDTO;
import com.bob.db.entity.CandidateDocumentsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateDocumentsMapper {

    CandidateDocumentsDTO toDto(CandidateDocumentsEntity candidateDocumentsEntity);

    CandidateDocumentsEntity toEntity(CandidateDocumentsDTO candidateDocumentsDTO);

    List<CandidateDocumentsDTO> toDTOList(List<CandidateDocumentsEntity> candidateDocumentStoreEntities);
    List<CandidateDocumentsEntity> toEntityList(List<CandidateDocumentsDTO> candidateDocumentStoreDTOS);
    void updateEntityFromDto(CandidateDocumentsDTO dto, @MappingTarget CandidateDocumentsEntity entity);
}
