package com.sentrifugo.rms.common.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Azure Blob storage under container {@code app.file-storage.blob.container} (default {@code rms}).
 * Object key: {@code {path-prefix}/{subFolder}/{fileName}} (path-prefix defaults to spring.application.name).
 */
@Slf4j
@Service
public class AzureBlobFileStorageService implements FileStorageService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${app.file-storage.blob.container:rms}")
    private String containerName;

    @Value("${app.file-storage.blob.path-prefix:${spring.application.name:rms}}")
    private String pathPrefix;

    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Created blob container '{}'", containerName);
        }
    }

    private String blobKey(String relativePath) {
        String prefix = pathPrefix == null ? "" : pathPrefix.replaceAll("^/+|/+$", "");
        String rel = relativePath.replaceAll("^/+", "");
        return prefix.isBlank() ? rel : prefix + "/" + rel;
    }

    @Override
    public String store(MultipartFile file, String subFolder) {
        try {
            String extension = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }
            String storedName = UUID.randomUUID() + extension;
            String relative = subFolder + "/" + storedName;
            upload(blobKey(relative), file.getInputStream(), file.getSize());
            return relative;
        } catch (IOException e) {
            throw new CommonException("Failed to store file in blob: " + e.getMessage());
        }
    }

    @Override
    public String storeBytes(byte[] bytes, String subFolder, String fileName) {
        String relative = subFolder + "/" + fileName;
        upload(blobKey(relative), new ByteArrayInputStream(bytes), bytes.length);
        return relative;
    }

    private void upload(String key, InputStream data, long size) {
        try {
            BlobClient blob = containerClient.getBlobClient(key);
            blob.upload(data, size, true);
        } catch (BlobStorageException e) {
            throw new CommonException("Failed to upload blob '" + key + "': " + e.getMessage());
        }
    }

    @Override
    public Resource load(String relativePath) {
        try {
            BlobClient blob = containerClient.getBlobClient(blobKey(relativePath));
            if (!blob.exists()) {
                throw new ResourceNotFoundException("File not found: " + relativePath);
            }
            InputStream in = blob.openInputStream();
            return new InputStreamResource(in) {
                @Override
                public String getFilename() {
                    int slash = relativePath.lastIndexOf('/');
                    return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
                }

                @Override
                public long contentLength() {
                    return blob.getProperties().getBlobSize();
                }
            };
        } catch (BlobStorageException e) {
            throw new ResourceNotFoundException("File not found: " + relativePath);
        }
    }

    @Override
    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            containerClient.getBlobClient(blobKey(relativePath)).deleteIfExists();
        } catch (Exception e) {
            log.warn("Failed to delete blob {}: {}", relativePath, e.getMessage());
        }
    }
}
