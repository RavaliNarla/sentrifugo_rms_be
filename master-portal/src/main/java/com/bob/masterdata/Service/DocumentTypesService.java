package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.DocumentTypesDTO;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.mapper.DocumentTypesMapper;
import com.bob.db.repository.DocumentTypesRepository;
import com.bob.commonutil.util.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentTypesService {
    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private DocumentTypesMapper documentTypesMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public DocumentTypesDTO addDocumentType(DocumentTypesDTO documentType) {
        DocumentTypesEntity entity = documentTypesMapper.toEntity(documentType);
        DocumentTypesEntity savedEntity = documentTypesRepository.save(entity);
        return documentTypesMapper.toDTO(savedEntity);
    }

    public List<DocumentTypesDTO> getAllDocumentTypes() {
        List<DocumentTypesEntity> entities = documentTypesRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE));
        return documentTypesMapper.toDTOList(entities);
    }

    public DocumentTypesDTO updateDocumentType(UUID id, DocumentTypesDTO documentType) {
        DocumentTypesEntity entity = documentTypesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document Type not found"));
        documentTypesMapper.updateEntityFromDto(documentType, entity);
        return documentTypesMapper.toDTO(documentTypesRepository.save(entity));
    }

    public DocumentTypesDTO deleteDocumentType(UUID id) {
        DocumentTypesEntity entity = documentTypesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document Type not found"));
        documentTypesRepository.deleteById(id);
        return documentTypesMapper.toDTO(entity);
    }

    public List<DocumentTypesDTO> bulkSave(MultipartFile file) {
        try {
            List<DocumentTypesDTO> documentTypesDTOS = excelTemplateService.excelToDto(file.getInputStream(), DocumentTypesDTO.class);
//            BulkValidationUtils.validateNoDuplicates(
//                    documentTypesDTOS,
//                    DocumentTypesDTO::getDocumentName,
//                    entityManager,
//                    DocumentTypesEntity.class,
//                    "documentName",
//                    "Document Type"
//            );
            List<DocumentTypesEntity> documentTypesEntities = documentTypesMapper.toEntityList(documentTypesDTOS);
            return documentTypesMapper.toDTOList(documentTypesRepository.saveAll(documentTypesEntities));
        } catch (IOException e) {
            throw new CommonException("fail to store excel data.");
        }
    }

    public List<DocumentTypesDTO> getDocumentTypesByType(String docType) {
        List<DocumentTypesEntity> entities = documentTypesRepository.findByDocType(docType);
        return documentTypesMapper.toDTOList(entities);
    }
}
