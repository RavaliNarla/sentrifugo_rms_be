package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.ZonalStatesDTO;
import com.bob.masterdata.Service.ZonalStatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${master.api.base.path}/zonal-states")
public class ZonalStatesController {

    @Autowired
    private ZonalStatesService zonalStatesService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ZonalStatesDTO>>> getAllZonalStates(){
        List<ZonalStatesDTO> zonalStatesDTOS = zonalStatesService.getAllZonalStates();
        ApiResponse<List<ZonalStatesDTO>> response = ApiResponse.ok(zonalStatesDTOS,"ZONAL STATES FETCHED SUCCESSFULLY!");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
