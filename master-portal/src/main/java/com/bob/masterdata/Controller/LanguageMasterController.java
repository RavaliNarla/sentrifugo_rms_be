package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.LanguageMasterDTO;
import com.bob.db.dto.StateLanguagesDTO;
import com.bob.masterdata.Model.StateLanguageModel;
import com.bob.masterdata.Service.LanguageMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/state-language")
public class LanguageMasterController {
    @Autowired
    private LanguageMasterService languageMasterService;

    @GetMapping("/get-languages")
    public ResponseEntity<ApiResponse<List<StateLanguagesDTO>>> getAllLanguages(@RequestBody List<UUID> stateIds){
        List<StateLanguagesDTO> languages = languageMasterService.getAllLanguagesByStateIds(stateIds);
        return ResponseEntity.ok(ApiResponse.ok(languages,"languages fetched successfully"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StateLanguagesDTO>>> getAllLanguages() throws Exception {
            List<StateLanguagesDTO> languages = languageMasterService.getAllLanguages();
            return new ResponseEntity<>(ApiResponse.ok(  languages,"DATA FIELDS FETCHED SUCCESSFULLY!"), HttpStatus.OK);
    }

    @PostMapping("/create-or-update/state-languages")
    public ResponseEntity<ApiResponse<List<StateLanguagesDTO>>> createOrUpdateStateLanguage(@RequestBody StateLanguageModel stateLanguageModel) {
        List<StateLanguagesDTO> res=languageMasterService.createOrUpdateStateLanguage(stateLanguageModel);
        return ResponseEntity.ok(ApiResponse.ok(res,"State languages added successfully"));
    }

}
