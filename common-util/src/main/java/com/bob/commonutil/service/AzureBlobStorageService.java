package com.bob.commonutil.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.FileContentTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class AzureBlobStorageService {


    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Value("${azure.storage.container-name:}")
    private String containerName;

    private BlobContainerClient getContainerClient() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    public String uploadFile(MultipartFile file, String fileName, String path) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            fileName = file.getOriginalFilename();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (fileName != null && !fileName.endsWith(extension)) {
                fileName = fileName + extension;
            }
        }

        BlobContainerClient containerClient = getContainerClient();
        String blobName = constructBlobName(fileName, path);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
            applyContentTypeOnly(blobClient, fileName);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw e;
        }
        return fileName;
    }

    public byte[] downloadFile(String fileName, String path) {

        BlobContainerClient containerClient = getContainerClient();
        String blobName = constructBlobName(fileName, path);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (blobClient.exists()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.download(outputStream);
            return outputStream.toByteArray();
        } else {
            log.warn("File not found: {}", blobName);
            return null;
        }
    }

    private String constructBlobName(String fileName, String path) {
        if (path != null && !path.isEmpty()) {
            return path + "/" + fileName;
        }
        return fileName;
    }

    public void deleteFile(String uploadDir, String baseFileName) {
        BlobContainerClient containerClient = getContainerClient();
        String blobName = constructBlobName(baseFileName, uploadDir);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try {
            if (blobClient.exists()) {
                blobClient.delete();
                log.info("File deleted successfully: {}", blobName);
            } else {
                log.warn("File not found for deletion: {}", blobName);
            }
        } catch (Exception e) {
            log.error("Failed to delete file: {}", blobName, e);
            throw e;
        }
    }

    public String generateReadSasUrl(String blobName) {

        BlobServiceClient blobServiceClient =
                new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();

        BlobClient blobClient =
                blobServiceClient
                        .getBlobContainerClient(containerName)
                        .getBlobClient(blobName);

        // Permissions (READ only)
        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);

        String filename = FileContentTypeUtil.extractFilename(blobName);
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentDisposition(
                        AppConstants.CONTENT_DISPOSITION_INLINE + "; filename=\"" + filename + "\""
                );
        String contentType = FileContentTypeUtil.getMimeTypeFromFilename(filename);
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        blobClient.setHttpHeaders(headers);


        // Expiry (10 minutes)
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10);

        BlobServiceSasSignatureValues values =
                new BlobServiceSasSignatureValues(expiryTime, permission);

        String sasToken = blobClient.generateSas(values);

        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    /** Sets blob Content-Type from filename; does not change Content-Disposition (view flow stays inline). */
    private void applyContentTypeOnly(BlobClient blobClient, String fileName) {
        String contentType = FileContentTypeUtil.getMimeTypeFromFilename(fileName);
        if (contentType == null) {
            return;
        }
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }

    public void downloadFilesAsZip(Collection<String> fileUrls, OutputStream outputStream) {
        BlobContainerClient containerClient = getContainerClient();

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            // Use a Set to track names and prevent duplicate ZipEntry exceptions
            Set<String> addedFileNames = new HashSet<>();

            for (String blobName : fileUrls) {
                if (blobName == null || blobName.trim().isEmpty()) {
                    continue;
                }

                BlobClient blobClient = containerClient.getBlobClient(blobName);

                if (blobClient.exists()) {
                    // Extract the file name from the URL (everything after the last '/')
                    String zipEntryName = blobName.contains("/")
                            ? blobName.substring(blobName.lastIndexOf('/') + 1)
                            : blobName;

                    // Prevent crashing if two files have the exact same name
                    if (!addedFileNames.add(zipEntryName)) {
                        log.warn("Duplicate filename found: {}. Skipping to prevent ZipException.", zipEntryName);
                        continue;
                    }

                    // Create a new entry in the ZIP file for this document
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);

                    // Stream the blob directly into the ZipOutputStream
                    blobClient.downloadStream(zos);

                    zos.closeEntry();
                    log.info("Successfully added {} to the zip archive.", zipEntryName);
                } else {
                    log.warn("File not found in Azure for zip download: {}", blobName);
                }
            }
            // Finish writing the zip file headers
            zos.finish();

        } catch (IOException e) {
            log.error("Error creating zip file stream", e);
            throw new RuntimeException("Failed to generate and stream zip file", e);
        }
    }
}