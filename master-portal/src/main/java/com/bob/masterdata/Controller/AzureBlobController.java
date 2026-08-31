package com.bob.masterdata.Controller;

import com.bob.commonutil.service.AzureBlobStorageService;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${master.api.base.path}/azureblob")
public class AzureBlobController {


    @Autowired
    AzureBlobStorageService azureBlobStorageService;

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file, @RequestParam("fileName") String fileName, @RequestParam("path") String path)
            throws IOException {
        String blobPath = azureBlobStorageService.uploadFile(file, fileName, path);
        return ResponseEntity.ok(ApiResponse.ok(blobPath, "File uploaded successfully"));
    }

    @GetMapping(value = "/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("path") String path){
        byte[] blobFile = azureBlobStorageService.downloadFile("", path);
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, AppConstants.CONTENT_DISPOSITION_ATTACHMENT+"; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(blobFile);
    }

    @GetMapping("/file/sas-url")
    public ResponseEntity<String> getFileSas( @RequestParam String dir) {
           String url = azureBlobStorageService.generateReadSasUrl(dir);
           return ResponseEntity.ok(url);
    }
}
