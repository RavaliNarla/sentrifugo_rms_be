package com.bob.candidateportal.controllers;

import com.bob.candidateportal.model.DigilockerDataResponseModel;
import com.bob.candidateportal.service.DigilockerService;
import com.bob.commonutil.exception.SchedulingConflictException;
import com.bob.db.dto.ApiResponse;
import com.bob.db.dto.EducationDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${candidate.api.base.path}/digilocker")
@Slf4j
public class DigilockerController {

    @Value("${digilocker.upload.success.url}")
    private String frontendSuccessUrl;

    @Autowired
    private DigilockerService digilockerService;

    @GetMapping("/init")
    public ResponseEntity<ApiResponse<?>> init(
            @RequestParam(name = "flowType", defaultValue = "uploaded") String flowType,
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "docCode") String docCode,
            @RequestParam(name="pageNo") Integer pageNo) throws Exception {


        String authorizationUrl = digilockerService.initiateAuthorization(flowType, scope,docCode,pageNo);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("redirect_uri", authorizationUrl);

        return ResponseEntity.ok(ApiResponse.ok(responseData, "Authorization URL generated successfully for flow: " + flowType));
    }

    @GetMapping("/handleCallback")
    public void handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {

        Integer selectedPageNo  = 0;
        String  selectedDocCode = "";

        try {
            List<DigilockerDataResponseModel> results =
                    digilockerService.processDigilockerCallback(code, state);

            selectedPageNo = results.get(0).getPageNo();

            List<String> succeeded = results.stream()
                    .filter(DigilockerDataResponseModel::getIsSuccess)
                    .map(DigilockerDataResponseModel::getDocCode)
                    .toList();

            List<String> failed = results.stream()
                    .filter(r -> !r.getIsSuccess())
                    .map(DigilockerDataResponseModel::getDocCode)
                    .toList();

            StringBuilder redirectURL = new StringBuilder(frontendSuccessUrl)
                    .append("?pageNo=").append(selectedPageNo)
                    .append("&status=success")
                    .append("&succeeded=").append(URLEncoder.encode(String.join(",", succeeded), StandardCharsets.UTF_8));

            // Partial failure — append failed list so frontend can show manual upload prompt
            if (!failed.isEmpty()) {
                redirectURL.append("&failed=")
                        .append(URLEncoder.encode(String.join(",", failed), StandardCharsets.UTF_8));
            }

            log.info("DigiLocker callback success. pageNo={}, succeeded={}, failed={}",
                    selectedPageNo, succeeded, failed);
            response.sendRedirect(redirectURL.toString());

        } catch (SchedulingConflictException e) {
            DigilockerDataResponseModel responseModel =
                    (DigilockerDataResponseModel) e.getResponse();
            selectedPageNo  = responseModel.getPageNo();
            selectedDocCode = responseModel.getDocCode();

            log.warn("DigiLocker all documents failed. pageNo={}, docCode={}, error={}",
                    selectedPageNo, selectedDocCode, e.getMessage());

            String redirectURL = frontendSuccessUrl
                    + "?pageNo="       + selectedPageNo
                    + "&docType="      + selectedDocCode
                    + "&status=error"
                    + "&errorMessage=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);

            response.sendRedirect(redirectURL);

        } catch (Exception e) {
            log.error("Unexpected error during DigiLocker callback", e);

            String redirectURL = frontendSuccessUrl
                    + "?status=error"
                    + "&errorMessage=" + URLEncoder.encode(
                    "Something went wrong. Please try again.", StandardCharsets.UTF_8);

            response.sendRedirect(redirectURL);
        }
    }
}
