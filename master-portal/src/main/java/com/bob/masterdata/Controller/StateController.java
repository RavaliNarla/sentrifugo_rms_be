package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.StateDTO;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.masterdata.Service.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/state")
public class StateController {
    @Autowired
    private StateService stateService;

    @Autowired
    private ExcelTemplateService excelTemplateService;


    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StateDTO>>> getAllStates(){
            List<StateDTO> states = stateService.getAllStates();
            ApiResponse<List<StateDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", states);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<StateDTO>> createState(@RequestBody StateDTO state){

            StateDTO state1 = stateService.createState(state);
            ApiResponse<StateDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", state1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<StateDTO>> updateState(@PathVariable UUID id, @RequestBody StateDTO state){
            StateDTO state1 = stateService.updateState(id, state);
            ApiResponse<StateDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", state1);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<StateDTO>> deleteState(@PathVariable UUID id){
            StateDTO state = stateService.deleteState(id);
            ApiResponse<StateDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", state);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }


}
