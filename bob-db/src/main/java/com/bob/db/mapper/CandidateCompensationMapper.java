package com.bob.db.mapper;

import com.bob.db.dto.CandidateCompensationDTO;
import com.bob.db.entity.CandidateCompensationEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CandidateApplicationsMapper.class, CandidateProfileMapper.class}
)
public interface CandidateCompensationMapper {

    // 1. TO DTO (READING)
    // MapStruct will use CandidateProfileMapper to fill the DTO object for the UI
    @Mapping(source = "candidateProfile", target = "candidateProfile")
    @Mapping(source = "application", target = "application")
    CandidateCompensationDTO toDTO(CandidateCompensationEntity entity);

    // 2. TO ENTITY (STORING)
    @Mapping(source = "application", target = "application")
    @Mapping(source = "candidateId", target = "candidateId") // Maps the raw UUID for storage
    @Mapping(target = "candidateProfile", ignore = true)     // CRITICAL: Ignore the object so we don't overwrite profile data
    CandidateCompensationEntity toEntity(CandidateCompensationDTO dto);

    List<CandidateCompensationDTO> toDTOList(List<CandidateCompensationEntity> entities);

    List<CandidateCompensationEntity> toEntityList(List<CandidateCompensationDTO> dtos);

    @InheritConfiguration(name = "toEntity")
    void updateEntityFromDto(CandidateCompensationDTO dto, @MappingTarget CandidateCompensationEntity entity);
}