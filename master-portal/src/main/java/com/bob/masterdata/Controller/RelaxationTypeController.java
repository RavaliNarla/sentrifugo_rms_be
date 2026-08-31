package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.RelaxationTypesDTO;
import com.bob.masterdata.Service.RelaxationTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/relaxation-type")
public class RelaxationTypeController {
    @Autowired
    private RelaxationTypeService relaxationTypeService;
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RelaxationTypesDTO>> addRelaxationType(@RequestBody RelaxationTypesDTO dto) {
        RelaxationTypesDTO created = relaxationTypeService.addRelaxationType(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Relaxation Type added successfully", created));

    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RelaxationTypesDTO>> updateRelaxationType(@PathVariable UUID id, @RequestBody RelaxationTypesDTO dto) {
        RelaxationTypesDTO updated = relaxationTypeService.updateRelaxationType(id,dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Relaxation Type updated successfully", updated));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RelaxationTypesDTO>>> getAllRelaxationTypes() {
        List<RelaxationTypesDTO> allRelaxationTypes = relaxationTypeService.getAllRelaxationTypes();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all Relaxation Types", allRelaxationTypes));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRelaxationType(@PathVariable UUID id) {
        relaxationTypeService.deleteRelaxationType(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Relaxation Type deleted successfully", null));
    }

}
