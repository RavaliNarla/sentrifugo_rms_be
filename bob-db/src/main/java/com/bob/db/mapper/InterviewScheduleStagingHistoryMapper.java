package com.bob.db.mapper;

import com.bob.db.dto.InterviewScheduleStagingHistoryDTO;
import com.bob.db.entity.InterviewScheduleStagingHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CandidateApplicationsMapper.class, CandidateProfileMapper.class}
)
public interface InterviewScheduleStagingHistoryMapper {

        InterviewScheduleStagingHistoryDTO toDto(InterviewScheduleStagingHistoryEntity entity);

        InterviewScheduleStagingHistoryEntity toEntity(InterviewScheduleStagingHistoryDTO dto);

        List<InterviewScheduleStagingHistoryDTO> toDtoList(List<InterviewScheduleStagingHistoryEntity> entities);

        List<InterviewScheduleStagingHistoryEntity> toEntityList(List<InterviewScheduleStagingHistoryDTO> dtos);
    }