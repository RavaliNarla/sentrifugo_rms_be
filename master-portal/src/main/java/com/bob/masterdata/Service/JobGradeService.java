package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.JobGradeDTO;
import com.bob.db.entity.JobGradeEntity;
import com.bob.db.mapper.JobGradeMapper;
import com.bob.db.repository.JobGradeRepository;
import com.bob.masterdata.Model.JobGradeExcelModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.masterdata.utils.BulkValidationUtils;
import com.bob.masterdata.validators.JobGradeValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JobGradeService {

    @Autowired
    private JobGradeRepository jobGradeRepository;

    @Autowired
    private JobGradeMapper jobGradeMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JobGradeValidator jobGradeValidator;
    public JobGradeDTO createJobGrade(JobGradeDTO jobGradeDto) {
        try {
            jobGradeRepository.save(jobGradeMapper.toEntity(jobGradeDto));
            return jobGradeDto;
        } catch (Exception e) {
            return null;
        }
    }

    public List<JobGradeDTO> getAllJobGrades(){
        try {
            return jobGradeMapper.toDtoList(jobGradeRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE)));
        } catch (Exception e) {
            throw new CommonException("Failed to fetch JobGrades");
        }
    }

    public JobGradeDTO updateJobGrade(UUID id, JobGradeDTO jobGradeDto) {
        JobGradeEntity jobGradeEntity=jobGradeRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("JobGrade not found"));
        jobGradeMapper.updateEntityFromDto(jobGradeDto,jobGradeEntity);
        jobGradeRepository.save(jobGradeEntity);
        return jobGradeMapper.toDto(jobGradeEntity);
    }

    public JobGradeDTO deleteJobGrade(UUID id) {

        JobGradeEntity jobGradeEntity =jobGradeRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("JobGrade not found"));
        jobGradeRepository.delete(jobGradeEntity);
        return jobGradeMapper.toDto(jobGradeEntity);

    }

    public List<JobGradeDTO> bulkSave(MultipartFile file) {
        try {
            List<JobGradeExcelModel> jobGradeExcelModels =
                    excelTemplateService.excelToDto(
                            file.getInputStream(),
                            JobGradeExcelModel.class
                    );

            List<String> errors = new ArrayList<>();

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            jobGradeExcelModels,
                            JobGradeExcelModel::getJobGradeCode,
                            entityManager,
                            JobGradeEntity.class,
                            "jobGradeCode",
                            "Job Grade",
                            "Job Grade Code"
                    )
            );

            errors.addAll(BulkValidationUtils.collectDuplicateErrors(
                    jobGradeExcelModels,
                    JobGradeExcelModel::getJobScale,
                    entityManager,
                    JobGradeEntity.class,
                    "jobScale",
                    "Job Grade",
                    "Job Scale"
            ));

            errors.addAll(
                    jobGradeValidator.collectValidationErrors(jobGradeExcelModels)
            );

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }
            List<JobGradeDTO> jobGradeDTOS =
                    excelToDtoList(jobGradeExcelModels);

            List<JobGradeEntity> jobGradeEntities =
                    jobGradeMapper.toEntityList(jobGradeDTOS);

            return jobGradeMapper.toDtoList(
                    jobGradeRepository.saveAll(jobGradeEntities)
            );

        } catch (IOException e) {
            throw new CommonException(
                    "Fail to store excel data."
            );
        }
    }
    public List<JobGradeDTO> excelToDtoList(List<JobGradeExcelModel> excelModels) {
        return excelModels.stream()
                .map(this::excelToDto)
                .toList();
    }
    public JobGradeDTO excelToDto(JobGradeExcelModel excel) {
        return JobGradeDTO.builder()
                .id(excel.getId())
                .jobGradeCode(excel.getJobGradeCode())
                .jobGradeDesc(excel.getJobGradeDesc())
                .jobScale(excel.getJobScale())
                .minSalary(
                        excel.getMinSalary() == null ? null :
                                new BigDecimal(excel.getMinSalary())
                )
                .maxSalary(
                        excel.getMaxSalary() == null ? null :
                                new BigDecimal(excel.getMaxSalary())
                )
                .build();
    }

}
