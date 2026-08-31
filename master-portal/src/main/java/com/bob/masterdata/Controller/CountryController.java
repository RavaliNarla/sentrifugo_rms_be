package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.CountryDTO;
import com.bob.masterdata.Service.CountryService;
import com.bob.commonutil.service.ExcelTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/country")
public class CountryController {
    @Autowired
    private CountryService countryService;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CountryDTO>>> getAllCountries(){
        List<CountryDTO> countries = countryService.getAllCountries();
        ApiResponse<List<CountryDTO>> response = new ApiResponse<>(true, "DATA FIELDS FETCHED SUCCESSFULLY!", countries);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryDTO>> createcountry(@RequestBody CountryDTO country){

        CountryDTO msg = countryService.createCountry(country);
        ApiResponse<CountryDTO> response = new ApiResponse<>(true, "DATA FIELDS CREATED SUCCESSFULLY!", msg);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryDTO>> updateCountry(@PathVariable UUID id, @RequestBody CountryDTO country){
        CountryDTO msg = countryService.updateCountry(id, country);
        ApiResponse<CountryDTO> response = new ApiResponse<>(true, "DATA FIELD UPDATED SUCCESSFULLY!", msg);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryDTO>> deleteCountry(@PathVariable UUID id){
        CountryDTO country = countryService.deleteCountry(id);
        ApiResponse<CountryDTO> response = new ApiResponse<>(true, "DATA FIELD DELETED SUCCESSFULLY!", country);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
