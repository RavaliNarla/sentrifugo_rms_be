package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.CcAvenueService;
import com.bob.db.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/cc-avenue-order")
public class CcAvenueController {

    @Autowired
    private CcAvenueService ccAvenueService;

    @PostMapping("/initiate")
    public ModelAndView initiateCcAvenuePayment(@RequestParam UUID applicationId, @RequestParam BigDecimal fee) {
        Map<String, Object> redirectData = ccAvenueService.initiatePayment(applicationId,fee);

        ModelAndView modelAndView = new ModelAndView("ccavenue_redirect"); // Refers to src/main/resources/templates/ccavenue_redirect.html
        modelAndView.addObject("ccavenueUrl", redirectData.get("ccavenueUrl"));
        modelAndView.addObject("ccavenueParams", redirectData.get("ccavenueParams"));

        return modelAndView;
    }

    @PostMapping("/handleCallback") // New endpoint for CCAvenue callback
    public RedirectView handleCcAvenueCallback(@RequestParam("encResp") String encResp) throws JsonMappingException {
        return ccAvenueService.handleCcAvenueCallback(encResp);
    }

    @PostMapping("/handleWebhook") // Server-to-server notify URL webhook from CCAvenue
    public ResponseEntity<String> handleCcAvenueWebhook(@RequestParam("encResp") String encResp) {
        return ccAvenueService.handleCcAvenueWebhook(encResp);
    }

    @GetMapping("/get-application-fee") // New endpoint for CCAvenue callback
    public ResponseEntity<ApiResponse<BigDecimal>> getApplicationFee(){
        BigDecimal fee = ccAvenueService.getApplicationFee();
        return new ResponseEntity<>(new ApiResponse<>(true, "FEE FETCHED SUCCESSFULLY!", fee), HttpStatus.OK);
    }
}
