package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.TemplatesDTO;
import com.bob.masterdata.Model.OfferLetterTemplateModel;
import com.bob.masterdata.Service.TemplatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequestMapping("${master.api.base.path}/templates")
@RestController
public class TemplatesController {

    @Autowired
    private TemplatesService templatesService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TemplatesDTO>>> getAllTemplates() {
        List<TemplatesDTO> templates = templatesService.getAllTemplates();
        ApiResponse<List<TemplatesDTO>> response = ApiResponse.ok(templates, "Templates retrieved successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/preview")
    public ResponseEntity<byte[]> getTemplateFile(@RequestParam(required = true) UUID templateId) {
        OfferLetterTemplateModel templateModel = templatesService.getPreviewContent(templateId);

        return  ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename="+templateModel.getTemplateName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(templateModel.getPreviewContent());
    }

    @GetMapping("/candidate-preview")
    public ResponseEntity<byte[]> getCandidatePreviewTemplate(@RequestParam(required = true) UUID templateId, @RequestParam(required = true) UUID applicationId) {
        OfferLetterTemplateModel templateModel = templatesService.getCandidatePreviewTemplate(templateId, applicationId);

        return  ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename="+templateModel.getTemplateName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(templateModel.getPreviewContent());
    }



    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String path) throws IOException {
        String response = templatesService.uploadTemplete(file, path);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }


}
