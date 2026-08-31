package com.bob.masterdata.Controller;


import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.DistrictDTO;
import com.bob.masterdata.Service.DistrictService;
import com.bob.commonutil.service.ExcelTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/districts")
public class DistrictController {

    @Autowired
    private DistrictService districtService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DistrictDTO>> addDistrict(
            @RequestBody DistrictDTO districtDTO) {

        DistrictDTO createdDistrict = districtService.addDistrict(districtDTO);
        ApiResponse<DistrictDTO> response =
                new ApiResponse<>(true, "District added successfully", createdDistrict);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllDistricts() {

        List<DistrictDTO> districts = districtService.getAllDistricts();
        ApiResponse<?> response =
                new ApiResponse<>(true, "Fetched all districts successfully", districts);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DistrictDTO>> updateDistrict(
            @PathVariable UUID id,
            @RequestBody DistrictDTO districtDTO) {

        DistrictDTO updatedDistrict = districtService.updateDistrict(id, districtDTO);
        ApiResponse<DistrictDTO> response =
                new ApiResponse<>(true, "District updated successfully", updatedDistrict);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DistrictDTO>> deleteDistrict(
            @PathVariable UUID id) {

        DistrictDTO deletedDistrict = districtService.deleteDistrict(id);
        ApiResponse<DistrictDTO> response =
                new ApiResponse<>(true, "District deleted successfully", deletedDistrict);
        return ResponseEntity.ok(response);
    }

}
