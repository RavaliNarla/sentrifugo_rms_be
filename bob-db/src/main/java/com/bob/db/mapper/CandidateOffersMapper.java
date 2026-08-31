package com.bob.db.mapper;

import com.bob.db.dto.CandidateOffersDTO;
import com.bob.db.entity.CandidateOffersEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateOffersMapper {

    @Mapping(source = "positionId", target = "jobPosition.id")
    @Mapping(source = "candidateId", target = "candidate.id")
    @Mapping(source = "locationId", target = "interviewCenter.id")
    @Mapping(source = "applicationId", target = "candidateApplication.id")
    CandidateOffersEntity toEntity(CandidateOffersDTO candidateOffersDTO);

    @Mapping(source = "jobPosition.id", target = "positionId")
    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "interviewCenter.id", target = "locationId")
    @Mapping(source = "candidateApplication.id", target = "applicationId")
    CandidateOffersDTO toDTO(CandidateOffersEntity candidateOffersEntity);

    List<CandidateOffersDTO> toDTOList(List<CandidateOffersEntity> candidateOffersEntities);

    List<CandidateOffersEntity> toEntityList(List<CandidateOffersDTO> candidateOffersDTOS);

    void updateEntityFromDto(CandidateOffersDTO candidateOffersDTO, @MappingTarget CandidateOffersEntity candidateOffersEntity);

}
