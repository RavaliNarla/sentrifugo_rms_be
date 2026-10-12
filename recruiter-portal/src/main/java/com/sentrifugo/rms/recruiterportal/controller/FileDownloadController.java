package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.common.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serves resumes, ID proofs, approval documents, and generated offer PDFs from blob storage.
 * Path is everything after "/files/" — previewed inline via this endpoint.
 */
@Tag(name = "File Preview / Download")
@RestController
@RequestMapping("${recruiter.api.base.path}/files")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Preview/download a stored file by its relative path")
    @GetMapping("/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) throws IOException {
        String fullPath = request.getRequestURI();
        String marker = "/files/";
        int idx = fullPath.indexOf(marker);
        String relativePath = UriUtils.decode(fullPath.substring(idx + marker.length()), StandardCharsets.UTF_8);

        Resource resource = fileStorageService.load(relativePath);
        String filename = resource.getFilename() != null ? resource.getFilename() : relativePath;
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        // Blob resources are streams — do not call resource.getFile() (local-disk only).
        if (filename.toLowerCase().endsWith(".pdf")) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else if (filename.toLowerCase().endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        }

        ResponseEntity.BodyBuilder ok = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        long len = resource.contentLength();
        if (len >= 0) {
            ok.contentLength(len);
        }
        return ok.body(resource);
    }
}
