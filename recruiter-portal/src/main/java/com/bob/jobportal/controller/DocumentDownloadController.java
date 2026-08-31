package com.bob.jobportal.controller;

import com.bob.jobportal.model.CommonScreeningDocumentDownloadRequestModel;
import com.bob.jobportal.service.DocumentDownloadService;
import com.bob.commonutil.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${recruiter.api.base.path}/candidate-details")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class DocumentDownloadController {

    @Autowired
    private DocumentDownloadService documentDownloadService;

    @PostMapping("/download")
    public ResponseEntity<byte[]> getAllDetails(@Valid @RequestBody CommonScreeningDocumentDownloadRequestModel model){
        byte[] detailsFile = documentDownloadService.getUserDetailsBasedOnRequestScreen(model);
        String fileName = model.getScreenName()+model.getDocumentType();


        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, AppConstants.CONTENT_DISPOSITION_ATTACHMENT+"; filename=" + fileName)
                .contentType( AppConstants.DOWNLOAD_DOC_TYPE_PDF.equalsIgnoreCase(model.getDocumentType()) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .body(detailsFile);

    }



}
