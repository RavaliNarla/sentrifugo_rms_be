package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.CertificationMasterDTO;
import com.bob.db.entity.CertificationMasterEntity;
import com.bob.db.mapper.CertificationMasterMapper;
import com.bob.db.repository.CertificationMasterRepository;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.util.AppConstants;
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
public class CertificationMasterService {

    @Autowired
    private CertificationMasterRepository certificationMasterRepository;

    @Autowired
    private CertificationMasterMapper certificationMasterMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;


    public CertificationMasterDTO saveCertificate(CertificationMasterDTO certificationMasterDTO){
            CertificationMasterEntity certificationMasterEntity = certificationMasterMapper.toEntity(certificationMasterDTO);
            CertificationMasterEntity savedCertificate = certificationMasterRepository.save(certificationMasterEntity);
            return certificationMasterMapper.toDTO(savedCertificate);
    }

    public List<CertificationMasterDTO> getAllCertificates(){
        List<CertificationMasterEntity> allCertificates = certificationMasterRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE));
        return certificationMasterMapper.toDTOList(allCertificates);
    }

    public CertificationMasterDTO getCertificateById(UUID id){
        CertificationMasterEntity certificate = certificationMasterRepository.findById(id)
                                                            .orElseThrow(()->new ResourceNotFoundException("certificate not found"));
        return certificationMasterMapper.toDTO(certificate);
    }


    public CertificationMasterDTO updateCertificate(UUID id,CertificationMasterDTO certificationMasterDTO){
        CertificationMasterEntity certificate = certificationMasterRepository.findById(id).
                                                orElseThrow(()->new ResourceNotFoundException("certificate not found"));
        certificationMasterMapper.updateEntityFromDTO(certificationMasterDTO,certificate);
        CertificationMasterEntity savedCertificate = certificationMasterRepository.save(certificate);
        return certificationMasterMapper.toDTO(savedCertificate);
    }


    public void deleteCertificate(UUID id){
        CertificationMasterEntity certificate = certificationMasterRepository.findById(id).
                orElseThrow(()->new ResourceNotFoundException("certificate doesn't exist"));
        certificationMasterRepository.deleteById(id);
    }

    public List<CertificationMasterDTO> bulkSave(MultipartFile file){
        try {
            List<CertificationMasterDTO> certificationMasterDTOS = excelTemplateService.excelToDto(file.getInputStream(), CertificationMasterDTO.class);

            List<String> errors = new ArrayList<>();
            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            certificationMasterDTOS,
                            CertificationMasterDTO::getCertificationName,
                            entityManager,
                            CertificationMasterEntity.class,
                            "certificationName",
                            "Certification Master",
                            "Certification Name"   // Excel column name
                    )
            );
            if(!errors.isEmpty()){
                throw new ExcelValidationException(errors);
            }
            List<CertificationMasterEntity> certificationMasterEntities = certificationMasterMapper.toEntityList(certificationMasterDTOS);
            return certificationMasterMapper.toDTOList(certificationMasterRepository.saveAll(certificationMasterEntities));
        } catch (IOException e) {
            throw new CommonException("fail to store excel data: " + e.getMessage());
        }
    }
}
