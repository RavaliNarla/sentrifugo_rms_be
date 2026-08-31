package com.bob.masterdata.Controller;

import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CertificationMasterDTO;
import com.bob.db.dto.DepartmentsDTO;
import com.bob.db.dto.UserDTO;
import com.bob.masterdata.Model.UserExcelModel;
import com.bob.masterdata.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers(){
            List<UserDTO> users = userService.getAllUsers();
            ApiResponse<List<UserDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", users);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody UserDTO userDTO){

            UserDTO user1 = userService.createUser(userDTO);
            ApiResponse<UserDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", user1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable UUID id,@RequestBody UserDTO userDTO){
            UserDTO user1 = userService.updateUser(id, userDTO);
            ApiResponse<UserDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", user1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> deleteUser(@PathVariable UUID id){
            UserDTO user = userService.deleteUser(id);
            ApiResponse<UserDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", user);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> searchUsers(
            @RequestParam @NotBlank(message = "Search term cannot be empty") @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size) {
        
        // Enforce pagination cap (defense in depth)
        if (size > 50) {
            size = 50;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<UserDTO> result = userService.searchUsers(search, pageable);
        ApiResponse<Page<UserDTO>> response = new ApiResponse<>(true, "Users searched successfully!", result);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
        ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(UserExcelModel.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(AppConstants.CONTENT_DISPOSITION_ATTACHMENT, excelTemplateFile.getFileName());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelTemplateFile.getFileContent());

    }

    @Operation(summary = "Bulk add Users from an Excel file")
    @PostMapping(value = "/bulk-add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }

        List<UserDTO> userDTOS =
                userService.bulkSave(file);

        ApiResponse<List<UserDTO>> response =
                new ApiResponse<>(
                        true,
                        "Uploaded the file successfully: " + file.getOriginalFilename(),
                        userDTOS
                );

        return ResponseEntity.ok(response);
    }
}
