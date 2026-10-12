package com.sentrifugo.rms.common.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores resumes, ID proofs, approval docs, and offer PDFs in Azure Blob.
 */
public interface FileStorageService {

    String store(MultipartFile file, String subFolder);

    String storeBytes(byte[] bytes, String subFolder, String fileName);

    Resource load(String relativePath);

    /** Best-effort delete; ignores missing files. */
    void deleteQuietly(String relativePath);
}
