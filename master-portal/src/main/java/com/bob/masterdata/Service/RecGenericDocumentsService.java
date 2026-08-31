package com.bob.masterdata.Service;

import com.bob.commonutil.service.AzureBlobStorageService;
import com.bob.db.dto.RecGenericDocumentsDTO;
import com.bob.db.entity.RecGenericDocumentsEntity;
import com.bob.db.mapper.RecGenericDocumentsMapper;
import com.bob.db.repository.RecGenericDocumentsRepository;
import com.bob.db.util.DBConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecGenericDocumentsService {
    @Value("${generic.documents.upload.dir}")
    private String genericDocumentsUploadDir;

    @Autowired
    private RecGenericDocumentsRepository recGenericDocumentsRepository;

    @Autowired
    AzureBlobStorageService azureBlobStorageService;



    @Autowired
    private RecGenericDocumentsMapper recGenericDocumentsMapper;


    public RecGenericDocumentsDTO saveGenericDocument(MultipartFile file, String type) throws IOException {
        if(file == null){
            throw new IllegalArgumentException("File cannot be null");
        }
        if (type == null || type.isEmpty() ){
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        Optional<RecGenericDocumentsEntity> entity = recGenericDocumentsRepository.findByType(type);
        RecGenericDocumentsEntity savedEntity = null;
        String newFileName =  Optional.ofNullable(file.getOriginalFilename())
                .orElse("unknown");
        String uploadFileName = type+"_"+UUID.randomUUID();
        String displayName =newFileName.split("\\.")[0];
        if(entity.isPresent()){
            RecGenericDocumentsEntity getEntity = entity.get();
            azureBlobStorageService.deleteFile(genericDocumentsUploadDir, getEntity.getFileName());
            String path = azureBlobStorageService.uploadFile(file, uploadFileName, genericDocumentsUploadDir);

            getEntity.setFileName(newFileName);
            getEntity.setFileUrl(genericDocumentsUploadDir+"/"+path);
            getEntity.setVersionNo(getEntity.getVersionNo()+1);
            getEntity.setDisplayName(displayName);
            savedEntity= recGenericDocumentsRepository.save(getEntity);
            return recGenericDocumentsMapper.toDto(savedEntity);
        }else{
            String path = azureBlobStorageService.uploadFile(file, newFileName, genericDocumentsUploadDir);
            RecGenericDocumentsEntity getEntity = RecGenericDocumentsEntity.builder()
                    .type(type)
                    .fileName(newFileName)
                    .fileUrl(genericDocumentsUploadDir+"/"+path)
                    .displayName(displayName)
                    .versionNo(1)
                    .build();
            savedEntity = recGenericDocumentsRepository.save(getEntity);

        }
        return recGenericDocumentsMapper.toDto(savedEntity);


    }
    public List<RecGenericDocumentsDTO> getAllGenericDocuments() {
        List<RecGenericDocumentsEntity> entities = recGenericDocumentsRepository.findAll(Sort.by(Sort.Direction.DESC, DBConstants.MASTER_CREATED_DATE));
        return recGenericDocumentsMapper.toDtoList(entities);
    }

    public List<RecGenericDocumentsDTO> getUniqueGenericDocuments() {
        List<RecGenericDocumentsEntity> entities = recGenericDocumentsRepository.findLatestOnePerType();
        return recGenericDocumentsMapper.toDtoList(entities);
    }

}
