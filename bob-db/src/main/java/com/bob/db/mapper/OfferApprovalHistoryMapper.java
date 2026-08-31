package com.bob.db.mapper;

import com.bob.db.dto.OfferApprovalHistoryDTO;
import com.bob.db.entity.OfferApprovalHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
                CandidateApplicationsMapper.class,
                CandidateProfileMapper.class
        }
)
public interface OfferApprovalHistoryMapper {

    @Mapping(target = "candidateProfile", ignore = true)
    OfferApprovalHistoryEntity toEntity(OfferApprovalHistoryDTO dto);

    OfferApprovalHistoryDTO toDTO(OfferApprovalHistoryEntity entity);

    List<OfferApprovalHistoryDTO> toDTOList(List<OfferApprovalHistoryEntity> entities);

    List<OfferApprovalHistoryEntity> toEntityList(List<OfferApprovalHistoryDTO> dtos);

    @Mapping(target = "candidateProfile", ignore = true)
    void updateEntityFromDto(OfferApprovalHistoryDTO dto, @MappingTarget OfferApprovalHistoryEntity entity);
}