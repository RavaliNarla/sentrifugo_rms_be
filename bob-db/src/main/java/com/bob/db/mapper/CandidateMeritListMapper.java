package com.bob.db.mapper;

import com.bob.db.dto.CandidateAddressDTO;
import com.bob.db.dto.CandidateMeritListDTO;
import com.bob.db.entity.CandidateAddressEntity;
import com.bob.db.entity.CandidateMeritListEntity;
import com.bob.db.model.MeritCandidateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CandidateMeritListMapper {

    CandidateMeritListEntity toEntity(CandidateMeritListDTO candidateMeritListDTO);

    CandidateMeritListDTO toDTO(CandidateMeritListEntity candidateMeritListEntity);

    List<CandidateMeritListDTO> toDTOList(List<CandidateMeritListEntity> candidateMeritListEntities);

    List<CandidateMeritListEntity> toEntityList(List<CandidateMeritListDTO> candidateMeritListDTOS);

    void updateEntityFromDto(CandidateMeritListDTO candidateMeritListDTO, @MappingTarget CandidateMeritListEntity candidateMeritListEntity);

    default MeritCandidateDTO toMeritCandidateDTO(CandidateMeritListEntity entity){
        return MeritCandidateDTO.builder()
                .candidateId(entity.getCandidateId())
                .applicationId(entity.getApplicationId())

                // Reservation
                .categoryId(entity.getCategoryId())
                .isPwd(entity.getIsPwd())
                .isExServicemen(entity.getIsExServicemen())

                // Location
                .stateId(entity.getStateId())
                .cityId(entity.getCityId())

                // Eligibility
                .generalEligible(entity.getGeneralEligible())

                // Scores
                .writtenScore(entity.getWrittenScore())
                .interviewScore(entity.getInterviewScore())
                .combinedScore(entity.getCombinedScore())
                .examWeightedScore(entity.getExamWeightedScore())
                .interviewWeightedScore(entity.getInterviewWeightedScore())

                // Selection
                .selectedCategoryId(entity.getSelectedCategoryId())
                .selected(entity.getSelected())
                .waitlisted(entity.getWaitlisted())

                // Ranking
                .overallRank(entity.getOverallRank())
                .categoryRank(entity.getCategoryRank())

                // PWD
                .selectedPwdCategoryId(entity.getSelectedPwdCategoryId())
                .selectedAgainstPwd(entity.getSelectedAgainstPwd())
                .isEWSSeat(entity.getIsEWSSeat())

                // DOB
                .dob(entity.getDob())
                .build();
    }

    default List<MeritCandidateDTO> toMeritCandidateDTOList(List<CandidateMeritListEntity> candidateMeritListEntities){
        return candidateMeritListEntities.stream()
                .map(this::toMeritCandidateDTO)
                .toList();
    }

    default List<CandidateMeritListEntity> toMeritCandidateEntityList(List<MeritCandidateDTO> meritCandidateDTOs,UUID positionId){
        return meritCandidateDTOs.stream()
                .map(toMeritCandidateEntity->toMeritCandidateEntity(toMeritCandidateEntity, positionId)) // positionId will be set later
                .toList();
    }

    default CandidateMeritListEntity toMeritCandidateEntity(MeritCandidateDTO dto, UUID positionId){
        return CandidateMeritListEntity.builder()
                .candidateId(dto.getCandidateId())
                .applicationId(dto.getApplicationId())
                .positionId(positionId)

                // Reservation
                .categoryId(dto.getCategoryId())
                .isPwd(dto.getIsPwd())
                .isExServicemen(dto.getIsExServicemen())

                // Location
                .stateId(dto.getStateId())
                .cityId(dto.getCityId())

                // Eligibility
                .generalEligible(dto.getGeneralEligible())

                // Scores
                .writtenScore(dto.getWrittenScore())
                .interviewScore(dto.getInterviewScore())
                .combinedScore(dto.getCombinedScore())
                .examWeightedScore(dto.getExamWeightedScore())
                .interviewWeightedScore(dto.getInterviewWeightedScore())

                // Selection
                .selectedCategoryId(dto.getSelectedCategoryId())
                .selected(dto.getSelected())
                .waitlisted(dto.getWaitlisted())

                // Ranking
                .overallRank(dto.getOverallRank())
                .categoryRank(dto.getCategoryRank())

                // PWD
                .selectedPwdCategoryId(dto.getSelectedPwdCategoryId())
                .selectedAgainstPwd(dto.getSelectedAgainstPwd())

                .isEWSSeat(dto.getIsEWSSeat())
                // DOB
                .dob(dto.getDob())
                .build();
    }


}
