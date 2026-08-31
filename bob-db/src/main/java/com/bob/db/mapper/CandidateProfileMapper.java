package com.bob.db.mapper;

import com.bob.db.dto.CandidateProfileDTO;
import com.bob.db.entity.CandidateProfileEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CandidateProfileMapper {


    CandidateProfileEntity toEntity(CandidateProfileDTO candidateProfileDTO);
    CandidateProfileDTO toDTO(CandidateProfileEntity candidateProfileEntity);
    List<CandidateProfileDTO> toDTOList(List<CandidateProfileEntity> candidateProfileEntities);
    List<CandidateProfileEntity> toEntityList(List<CandidateProfileDTO> candidateProfileDTOS);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "candidateId", ignore = true)
    void updateEntityFromDto(
            CandidateProfileDTO candidateProfileDTO, @MappingTarget CandidateProfileEntity candidateProfileEntity
    );

}
