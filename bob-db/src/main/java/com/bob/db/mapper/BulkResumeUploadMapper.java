package com.bob.db.mapper;

import com.bob.db.dto.BulkResumeUploadDTO;
import com.bob.db.entity.BulkResumeUploadEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BulkResumeUploadMapper {
    BulkResumeUploadDTO toDTO(BulkResumeUploadEntity entity);
    BulkResumeUploadEntity toEntity(BulkResumeUploadDTO dto);
}
