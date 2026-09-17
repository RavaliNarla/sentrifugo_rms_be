package com.sentrifugo.rms.common.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Simple local-disk file storage for this preview build (resumes, ID proofs, approval
 * documents, generated offer PDFs). No Azure Blob dependency.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file-storage.base-path:./uploads}")
    private String basePath;

    public String store(MultipartFile file, String subFolder) {
        try {
            Path dir = Paths.get(basePath, subFolder);
            Files.createDirectories(dir);
            String extension = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }
            String storedName = UUID.randomUUID() + extension;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return subFolder + "/" + storedName;
        } catch (IOException e) {
            throw new CommonException("Failed to store file: " + e.getMessage());
        }
    }

    public String storeBytes(byte[] bytes, String subFolder, String fileName) {
        try {
            Path dir = Paths.get(basePath, subFolder);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, bytes);
            return subFolder + "/" + fileName;
        } catch (IOException e) {
            throw new CommonException("Failed to store file: " + e.getMessage());
        }
    }

    public Resource load(String relativePath) {
        try {
            Path file = Paths.get(basePath, relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File not found: " + relativePath);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + relativePath);
        }
    }

    /** Best-effort delete; ignores missing files and logs I/O failures. */
    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path base = Paths.get(basePath).toAbsolutePath().normalize();
            Path file = base.resolve(relativePath).normalize();
            if (!file.startsWith(base)) {
                log.warn("Refusing to delete path outside storage root: {}", relativePath);
                return;
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", relativePath, e.getMessage());
        }
    }
}
