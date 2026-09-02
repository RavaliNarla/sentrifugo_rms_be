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
import java.nio.file.Files;

/**
 * Serves resumes, ID proofs, approval documents, and generated offer PDFs stored on
 * local disk. Path is everything after "/files/" - candidate resumes and offer PDFs are
 * previewed inline (PDF viewer popup) via this endpoint.
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
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
