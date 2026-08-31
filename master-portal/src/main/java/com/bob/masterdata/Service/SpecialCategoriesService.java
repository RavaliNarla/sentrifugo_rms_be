package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.SpecialCategoriesDTO;
import com.bob.db.entity.SpecialCategoriesEntity;
import com.bob.db.mapper.SpecialCategoriesMapper;
import com.bob.db.repository.SpecialCategoriesRepository;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.masterdata.utils.BulkValidationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SpecialCategoriesService {
    @Autowired
    private SpecialCategoriesRepository specialCategoriesRepository;

    @Autowired
    private SpecialCategoriesMapper specialCategoriesMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;
    public SpecialCategoriesDTO createSpecialCategory(SpecialCategoriesDTO specialCategoriesDto) {
        try {
            SpecialCategoriesEntity specialCategoriesEntity = specialCategoriesMapper.toEntity(specialCategoriesDto);
            specialCategoriesRepository.save(specialCategoriesEntity);
            return specialCategoriesMapper.toDto(specialCategoriesEntity);
        } catch (Exception e) {
            return null;
        }
    }

    public List<SpecialCategoriesDTO> getAllSpecialCategories(){
        try {
            return specialCategoriesMapper.toDtoList(specialCategoriesRepository.findAll(Sort.by(Sort.Direction.DESC,
                    AppConstants.MASTER_CREATED_DATE)));
        } catch (Exception e) {
            throw new CommonException("Failed to fetch categories");
        }
    }

    public SpecialCategoriesDTO updateSpecialCategory(UUID id, SpecialCategoriesDTO specialCategoriesDto) {
        SpecialCategoriesEntity specialCategoriesEntity=specialCategoriesRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Category not found"));
        specialCategoriesMapper.updateEntityFromDto(specialCategoriesDto,specialCategoriesEntity);
        specialCategoriesRepository.save(specialCategoriesEntity);
        return specialCategoriesMapper.toDto(specialCategoriesEntity);
    }

    public SpecialCategoriesDTO deleteCategory(UUID id) {
        SpecialCategoriesEntity specialCategoriesEntity =specialCategoriesRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Special Category not found"));
        SpecialCategoriesDTO specialCategoriesDto =specialCategoriesMapper.toDto(specialCategoriesEntity);
        specialCategoriesRepository.delete(specialCategoriesEntity);
        return specialCategoriesDto;

    }

    public List<SpecialCategoriesDTO> bulkSave(MultipartFile file) {
        try {
            List<SpecialCategoriesDTO> specialCategoriesDTOS =
                    excelTemplateService.excelToDto(
                            file.getInputStream(),
                            SpecialCategoriesDTO.class
                    );

            List<String> errors = new ArrayList<>();

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            specialCategoriesDTOS,
                            SpecialCategoriesDTO::getSpecialCategoryName,
                            entityManager,
                            SpecialCategoriesEntity.class,
                            "specialCategoryName",
                            "Special Category",
                            "Special Category Name"
                    )
            );

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            specialCategoriesDTOS,
                            SpecialCategoriesDTO::getSpecialCategoryCode,
                            entityManager,
                            SpecialCategoriesEntity.class,
                            "specialCategoryCode",
                            "Special Category",
                            "Special Category Code"
                    )
            );
            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }

            List<SpecialCategoriesEntity> entities =
                    specialCategoriesMapper.toEntityList(specialCategoriesDTOS);

            return specialCategoriesMapper.toDtoList(
                    specialCategoriesRepository.saveAll(entities)
            );

        } catch (IOException e) {
            throw new CommonException(
                    "Fail to store excel data.");
        }
    }



}
