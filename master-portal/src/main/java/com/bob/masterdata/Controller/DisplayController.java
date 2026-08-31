package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.masterdata.Model.GetCompleteDataResponse;
import com.bob.masterdata.Service.DisplayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("${master.api.base.path}/display")
public class DisplayController {
    @Autowired
    private DisplayService displayService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<GetCompleteDataResponse>> getCompleteData2(){
        GetCompleteDataResponse res=  displayService.getAllData();
        return ResponseEntity.ok(ApiResponse.ok(res,"Data fetched successfully"));
    }

    // TODO:need to check
//    @GetMapping("/file")
//    public String getAllCities(@RequestParam String filePath) throws IOException {
//            Path path = Paths.get(filePath);
//            return Files.readString(path);
//    }

}
