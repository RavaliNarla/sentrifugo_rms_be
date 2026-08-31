package com.bob.db.mapper;

import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.PositionStatus;
import com.bob.db.model.JobPositionsExcelModel;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // This handles ignoring unmapped target properties
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {/*PositionRequiredDocumentsMapper.class,*/ PositionStateDistributionMapper.class, PositionCategoryNationalDistributionMapper.class,JobPositionExclusionsMapper.class})
public abstract class JobPositionsMapper {

//    @Autowired
//    private PositionRequiredDocumentsMapper positionRequiredDocumentsMapper;
    @Autowired
    private PositionStateDistributionMapper positionStateDistributionMapper;
    @Autowired
    private PositionCategoryNationalDistributionMapper positionCategoryNationalDistributionMapper;

    @Autowired
    private JobPositionExclusionsMapper jobPositionExclusionsMapper;

    public abstract JobPositionsDTO toDto(JobPositionsEntity entity);

    public abstract JobPositionsEntity toEntity(JobPositionsDTO dto);

    public abstract List<JobPositionsDTO> toDtoList(List<JobPositionsEntity> entities);

    public abstract List<JobPositionsEntity> toEntityList(List<JobPositionsDTO> dtos);

    @Named("withoutChildren")
//    @Mapping(target = "positionRequiredDocuments", ignore = true)
    @Mapping(target = "positionStateDistributions", ignore = true)
    @Mapping(target = "positionCategoryNationalDistributions", ignore = true)
    @Mapping(target = "jobPositionExclusion",ignore = true)
    public abstract JobPositionsDTO toDtoWithoutChildren(JobPositionsEntity entity);

    @Named("withoutChildrenList")
    public abstract List<JobPositionsDTO> toDtoListWithoutChildren(List<JobPositionsEntity> entities);

//    @Mapping(target = "positionRequiredDocuments", ignore = true)
    @Mapping(target = "positionStateDistributions", ignore = true)
    public abstract void updateEntityFromDto(JobPositionsDTO dto, @MappingTarget JobPositionsEntity entity);

    @AfterMapping
    public void handleCollectionsOnUpdate(JobPositionsDTO dto, @MappingTarget JobPositionsEntity entity) {
        // Update PositionRequiredDocuments
//        entity.clearPositionRequiredDocuments();
//        if (!CollectionUtils.isEmpty(dto.getPositionRequiredDocuments())) {
//            List<PositionRequiredDocumentsEntity> docs = positionRequiredDocumentsMapper.toEntityList(dto.getPositionRequiredDocuments());
//            docs.forEach(doc -> doc.setJobPosition(entity));
//            entity.getPositionRequiredDocuments().addAll(docs);
//        }

        // Update PositionStateDistributions
        entity.clearPositionStateDistributions();
        if (!CollectionUtils.isEmpty(dto.getPositionStateDistributions())) {
            List<PositionStateDistributionEntity> states = positionStateDistributionMapper.toEntityList(dto.getPositionStateDistributions());
            states.forEach(state -> {
                state.setJobPosition(entity);
                if (state.getPositionCategoryDistributions() != null) {
                    state.getPositionCategoryDistributions().forEach(cat -> {
                        cat.setPositionStateDistribution(state);
                    });
                }
            });
            entity.getPositionStateDistributions().addAll(states);
        }

        // Update PositionCategoryNationalDistributions
        entity.clearPositionCategoryNationalDistributions();
        if (!CollectionUtils.isEmpty(dto.getPositionCategoryNationalDistributions())) {
            List<PositionCategoryNationalDistributionEntity> docs = positionCategoryNationalDistributionMapper.toEntityList(dto.getPositionCategoryNationalDistributions());
            docs.forEach(doc -> doc.setJobPosition(entity));
            entity.getPositionCategoryNationalDistributions().addAll(docs);
        }

        //updating exclusions
        entity.clearJobPositionExclusions();
        if (!CollectionUtils.isEmpty(dto.getJobPositionExclusion())) {
            List<JobPositionExclusionsEntity> exclusions = jobPositionExclusionsMapper.toEntityList(dto.getJobPositionExclusion());
            exclusions.forEach(exclusion -> exclusion.setJobPosition(entity));
            entity.getJobPositionExclusion().addAll(exclusions);
        }

    }

    @Mapping(target = "positionStatus", source = "positionStatus")
    public abstract JobPositionsEntity toEntity(JobPositionsExcelModel excelModel, PositionStatus positionStatus);
}
