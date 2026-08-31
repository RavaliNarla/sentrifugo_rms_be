package com.bob.db.mapper;

import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateApplicationsMapper {

    CandidateApplicationsEntity toEntity(CandidateApplicationsDTO dto);

    CandidateApplicationsDTO toDTO(CandidateApplicationsEntity entity);

    void updateEntityFromDto(CandidateApplicationsDTO dto,@MappingTarget CandidateApplicationsEntity entity);

    List<CandidateApplicationsEntity> toEntityList(List<CandidateApplicationsDTO> dtoList);

    List<CandidateApplicationsDTO> toDTOList(List<CandidateApplicationsEntity> entityList);
}
