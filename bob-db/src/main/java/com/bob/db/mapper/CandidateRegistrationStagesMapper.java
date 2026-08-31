package com.bob.db.mapper;

import com.bob.db.dto.CandidateRegistrationStagesDTO;
import com.bob.db.entity.CandidateRegistrationStagesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateRegistrationStagesMapper {

    CandidateRegistrationStagesEntity toEntity(CandidateRegistrationStagesDTO candidateRegistrationStagesDTO);

    CandidateRegistrationStagesDTO toDTO(CandidateRegistrationStagesEntity candidateRegistrationStagesEntity);

    List<CandidateRegistrationStagesDTO> toDTOList(List<CandidateRegistrationStagesEntity> candidateRegistrationStagesEntityList);

    List<CandidateRegistrationStagesEntity> toEntityList(List<CandidateRegistrationStagesDTO> candidateRegistrationStagesDTOList);

    void updateEntityFromDto(CandidateRegistrationStagesDTO candidateRegistrationStagesDTO, @MappingTarget CandidateRegistrationStagesEntity candidateRegistrationStagesEntity);

}
