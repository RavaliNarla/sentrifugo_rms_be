package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.EducationQualificationsDTO;
import com.bob.db.entity.EducationQualificationsEntity;
import com.bob.db.mapper.EducationQualificationsMapper;
import com.bob.db.repository.EducationQualificationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EducationalQualificationsService {
    @Autowired
    private EducationQualificationsRepository educationalQualificationsRepository;

    @Autowired
    private EducationQualificationsMapper educationalQualificationsMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    public EducationQualificationsDTO createEduQual(EducationQualificationsDTO educationQualificationsDto) {
        try {
            EducationQualificationsEntity educationQualificationsEntity = educationalQualificationsMapper.toEntity(educationQualificationsDto);
            ;
            return educationalQualificationsMapper.toDTO(educationalQualificationsRepository.save(educationQualificationsEntity));
        } catch (Exception e) {
            return null;
        }
    }

    public List<EducationQualificationsDTO> getAllEduQual(){
        try {
            return educationalQualificationsMapper.toDTOList(educationalQualificationsRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch Qualifications");
        }
    }

    public EducationQualificationsDTO updateEduQual(UUID id, EducationQualificationsDTO educationQualificationsDto) {
        EducationQualificationsEntity educationQualificationsEntity=educationalQualificationsRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Qualification not found"));
        educationalQualificationsMapper.updateEntityFromDto(educationQualificationsDto,educationQualificationsEntity);
        return educationalQualificationsMapper.toDTO(educationalQualificationsRepository.save(educationQualificationsEntity));
    }

    public EducationQualificationsDTO deleteEduQual(UUID id) {

                EducationQualificationsEntity educationQualificationsEntity =educationalQualificationsRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Qualification not found"));
                educationalQualificationsRepository.delete(educationQualificationsEntity);
                return educationalQualificationsMapper.toDTO(educationQualificationsEntity);
    }


}
