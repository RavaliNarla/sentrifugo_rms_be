//package com.bob.db.mapper;
//
//import com.bob.db.dto.JobPostingLocationDTO;
//import com.bob.db.entity.JobPostingLocationEntity;
//import org.mapstruct.Mapper;
//import org.mapstruct.NullValuePropertyMappingStrategy;
//import org.mapstruct.ReportingPolicy;
//
//import java.util.List;
//
//@Mapper(
//        componentModel = "spring",
//        unmappedTargetPolicy = ReportingPolicy.IGNORE,
//        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//public interface JobPostingLocationMapper {
//
//    JobPostingLocationDTO toDto(JobPostingLocationEntity entity);
//
//    JobPostingLocationEntity toEntity(JobPostingLocationDTO dto);
//
//    List<JobPostingLocationDTO> toDTOList(List<JobPostingLocationEntity> entities);
//
//    List<JobPostingLocationEntity> toEntityList(List<JobPostingLocationDTO> dtos);
//}
