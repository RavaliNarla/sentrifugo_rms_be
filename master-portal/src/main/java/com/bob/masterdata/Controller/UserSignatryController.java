package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.UserSignatryDto;
import com.bob.db.dto.ZonalStatesDTO;
import com.bob.masterdata.Service.UserSignatryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${master.api.base.path}/user-signatry")
public class UserSignatryController {

    @Autowired
    private UserSignatryService userSignatryService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserSignatryDto>>> getAllUserSignatries(){
        List<UserSignatryDto> userSignatryDtos = userSignatryService.getAllSignatries();
        return new ResponseEntity<>(ApiResponse.ok(userSignatryDtos,"User Signatries fetched successfully!"), HttpStatus.OK);
    }
}
