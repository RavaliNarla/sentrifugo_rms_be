package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CityDTO;
import com.bob.masterdata.Service.CityService;
import com.bob.commonutil.service.ExcelTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/city")
public class CityController {
    @Autowired
    private CityService cityService;

    @Autowired
    private ExcelTemplateService excelTemplateService;


    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CityDTO>>> getAllCities(){
            List<CityDTO> cities = cityService.getAllCities();
            ApiResponse<List<CityDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", cities);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CityDTO>> createCity(@RequestBody CityDTO city){

            CityDTO msg = cityService.createCity(city);
            ApiResponse<CityDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CityDTO>> updateCity(@PathVariable UUID id, @RequestBody CityDTO city){
            CityDTO msg = cityService.updateCities(id, city);
            ApiResponse<CityDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CityDTO>> deleteCity(@PathVariable UUID id){
            CityDTO city = cityService.deleteCities(id);
            ApiResponse<CityDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", city);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
