package com.bob.db.mapper;

import com.bob.db.dto.InterviewScheduleStagingDTO;
import com.bob.db.entity.InterviewScheduleStagingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CandidateApplicationsMapper.class, CandidateProfileMapper.class}
)
public interface InterviewScheduleStagingMapper {

    @Mapping(source = "candidateProfile", target = "candidateProfile")
    @Mapping(source = "application", target = "application")
    @Mapping(source = "interviewCentre", target = "interviewCentre")
    InterviewScheduleStagingDTO toDto(InterviewScheduleStagingEntity entity);

    @Mapping(source = "application", target = "application")
    @Mapping(source = "candidateId", target = "candidateId")
    @Mapping(target = "candidateProfile", ignore = true)
    @Mapping(target = "interviewCentre",ignore = true)
    InterviewScheduleStagingEntity toEntity(InterviewScheduleStagingDTO dto);

    List<InterviewScheduleStagingDTO> toDtoList(List<InterviewScheduleStagingEntity> entities);

    List<InterviewScheduleStagingEntity> toEntityList(List<InterviewScheduleStagingDTO> dtos);
}