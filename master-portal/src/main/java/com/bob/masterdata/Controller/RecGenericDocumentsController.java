package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.MasterPositionsDTO;
import com.bob.db.dto.RecGenericDocumentsDTO;
import com.bob.masterdata.Service.RecGenericDocumentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("${master.api.base.path}/rec-generic-documents")
public class RecGenericDocumentsController {

    @Autowired
    private RecGenericDocumentsService recGenericDocumentsService;

    @PostMapping(value = "/save-generic-document/{type}",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RecGenericDocumentsDTO>> saveGenericDocument(@PathVariable String type, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(recGenericDocumentsService.saveGenericDocument( file, type),"File saved successfully"));
    }
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RecGenericDocumentsDTO>>> getAllGenericDocuments() {
        List<RecGenericDocumentsDTO> list = recGenericDocumentsService.getAllGenericDocuments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", list));
    }
    @GetMapping("/unique-document-types")
    public ResponseEntity<ApiResponse<List<RecGenericDocumentsDTO>>> getUniqueGenericDocuments() {
        List<RecGenericDocumentsDTO> list = recGenericDocumentsService.getUniqueGenericDocuments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", list));
    }

}
