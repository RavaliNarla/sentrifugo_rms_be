package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.DepartmentsDTO;
import com.bob.db.entity.DepartmentsEntity;
import com.bob.db.mapper.DepartmentsMapper;
import com.bob.db.repository.DepartmentsRepository;
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
public class DepartmentsService {
    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private DepartmentsMapper departmentsMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public DepartmentsDTO createDepartment(DepartmentsDTO departments) {
        try {
            DepartmentsEntity departmentsEntity = departmentsMapper.toEntity(departments);
            departmentsRepository.save(departmentsEntity);
            return departmentsMapper.toDto(departmentsEntity);
        } catch (Exception e) {
            return null;
        }
    }

    public List<DepartmentsDTO> getAllDepartments() {
        try {
            return departmentsMapper.toDtoList(departmentsRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE)));
        } catch (Exception e) {
            throw new CommonException("Failed to fetch departments");
        }
    }

    public DepartmentsDTO updateDepartments(UUID id, DepartmentsDTO departmentsDto) {
        DepartmentsEntity departments=departmentsRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Department not found"));
        departmentsMapper.updateEntityFromDto(departmentsDto,departments);
        departmentsRepository.save(departments);
        return departmentsMapper.toDto(departments);
    }

    public DepartmentsDTO deleteDepartments(UUID id) {

        DepartmentsEntity department=departmentsRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Department not found"));
        departmentsRepository.delete(department);
        return departmentsMapper.toDto(department);
    }

    public List<DepartmentsDTO> bulkSave(MultipartFile file) {
        try {
            List<DepartmentsDTO> departmentsDTOs =
                    excelTemplateService.excelToDto(
                            file.getInputStream(),
                            DepartmentsDTO.class
                    );

            List<String> errors = new ArrayList<>();

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            departmentsDTOs,
                            DepartmentsDTO::getDepartmentName,
                            entityManager,
                            DepartmentsEntity.class,
                            "departmentName",
                            "Department",
                            "Department Name"
                    )
            );

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }

            List<DepartmentsEntity> departmentsEntities =
                    departmentsMapper.toEntityList(departmentsDTOs);

            return departmentsMapper.toDtoList(
                    departmentsRepository.saveAll(departmentsEntities)
            );

        } catch (IOException e) {
            throw new CommonException(
                    "Fail to store excel data");
        }
    }


}
