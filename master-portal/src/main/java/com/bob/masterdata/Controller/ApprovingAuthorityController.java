package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ApprovingAuthorityDTO;
import com.bob.masterdata.Service.ApprovingAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/approving-authority")
public class ApprovingAuthorityController {

    @Autowired
    private ApprovingAuthorityService approvingAuthorityService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ApprovingAuthorityDTO>>> getAll(){
        List<ApprovingAuthorityDTO> list = approvingAuthorityService.getAll();
        ApiResponse<List<ApprovingAuthorityDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", list);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<ApprovingAuthorityDTO>> create(@RequestBody ApprovingAuthorityDTO dto) {
        ApprovingAuthorityDTO created = approvingAuthorityService.create(dto);
        ApiResponse<ApprovingAuthorityDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", created);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<ApprovingAuthorityDTO>> update(@PathVariable UUID id, @RequestBody ApprovingAuthorityDTO dto) {
        ApprovingAuthorityDTO updated = approvingAuthorityService.update(id, dto);
        ApiResponse<ApprovingAuthorityDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", updated);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<ApprovingAuthorityDTO>> delete(@PathVariable UUID id) {
        ApprovingAuthorityDTO deleted = approvingAuthorityService.delete(id);
        ApiResponse<ApprovingAuthorityDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", deleted);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
