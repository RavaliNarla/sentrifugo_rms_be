package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.MasterPositionsDTO;
import com.bob.db.entity.MasterPositionsEntity;
import com.bob.db.mapper.MasterPositionsMapper;
import com.bob.db.repository.JobGradeRepository;
import com.bob.db.repository.MasterPositionsRepository;
import com.bob.masterdata.Model.MasterPositionExcelModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.masterdata.utils.BulkValidationUtils;
import com.bob.masterdata.validators.MasterPositionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MasterPositionsService  {

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobGradeRepository jobGradeRepository;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private MasterPositionValidator masterPositionValidator;

    @PersistenceContext
    private EntityManager entityManager;
    @Transactional
    public MasterPositionsDTO create(MasterPositionsDTO dto) {
        MasterPositionsEntity entity = masterPositionsMapper.toEntity(dto);
        MasterPositionsEntity saved = masterPositionsRepository.save(entity);
        return masterPositionsMapper.toDTO(saved);
    }

    @Transactional
    public MasterPositionsDTO update(UUID id, MasterPositionsDTO dto) {
        Optional<MasterPositionsEntity> opt = masterPositionsRepository.findById(id);
        if (opt.isEmpty()) throw new ResourceNotFoundException("MasterPosition not found");
        MasterPositionsEntity entity = opt.get();
        masterPositionsMapper.updateEntityFromDto(dto, entity);
        MasterPositionsEntity saved = masterPositionsRepository.save(entity);
        return masterPositionsMapper.toDTO(saved);
    }

    public List<MasterPositionsDTO> getAll() {
        return masterPositionsRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE)).stream().map(masterPositionsMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        masterPositionsRepository.deleteById(id);
    }

    public List<MasterPositionsDTO> bulkSave(MultipartFile file) {
        try {
            List<MasterPositionExcelModel> excelModels =
                    excelTemplateService.excelToDto(
                            file.getInputStream(),
                            MasterPositionExcelModel.class
                    );
//            System.out.println("Parsed Excel Models: " + excelModels);
            List<String> errors = new ArrayList<>();

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            excelModels,
                            MasterPositionExcelModel::getPositionName,
                            entityManager,
                            MasterPositionsEntity.class,
                            "positionName",
                            "Master Position",
                            "Position Title"
                    )
            );

            errors.addAll(
                    masterPositionValidator.collectValidationErrorsFromExcel(excelModels)
            );

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }

            List<MasterPositionsDTO> dtos =
                    excelModels.stream()
                            .map(this::mapToDTO)
                            .toList();

            List<MasterPositionsEntity> entities =
                    masterPositionsMapper.toEntityList(dtos);

            return masterPositionsMapper.toDTOList(
                    masterPositionsRepository.saveAll(entities)
            );

        } catch (IOException e) {
            throw new CommonException(
                    "Fail to store excel data"
            );
        }
    }

    private MasterPositionsDTO mapToDTO(MasterPositionExcelModel excel) {
        MasterPositionsDTO dto = new MasterPositionsDTO();

        dto.setPositionName(excel.getPositionName());
        dto.setDeptId(excel.getDeptId());
        dto.setEligibilityAgeMin(Integer.parseInt(excel.getEligibilityAgeMin()));
        dto.setEligibilityAgeMax(Integer.parseInt(excel.getEligibilityAgeMax()));
        dto.setGradeId(excel.getGradeId());
//        dto.setMandatoryEducation(excel.getMandatoryEducation());
//        dto.setPreferredEducation(excel.getPreferredEducation());
        dto.setMandatoryExperience(excel.getMandatoryExperience());
        dto.setPreferredExperience(excel.getPreferredExperience());
        dto.setRolesResponsibilities(excel.getRolesResponsibilities());

        return dto;
    }

}
