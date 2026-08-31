package com.bob.db.mapper;

import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CandidateDocumentStoreMapper {
    CandidateDocumentStoreEntity toEntity(CandidateDocumentStoreDTO candidateDocumentStoreDTO);
    CandidateDocumentStoreDTO toDTO(CandidateDocumentStoreEntity candidateDocumentStoreEntity);
    List<CandidateDocumentStoreDTO> toDTOList(List<CandidateDocumentStoreEntity> candidateDocumentStoreEntities);
    List<CandidateDocumentStoreEntity> toEntityList(List<CandidateDocumentStoreDTO> candidateDocumentStoreDTOS);
    void updateEntityFromDto(CandidateDocumentStoreDTO dto, @MappingTarget CandidateDocumentStoreEntity entity);
}
