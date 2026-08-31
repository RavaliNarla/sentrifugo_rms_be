package com.bob.candidateportal.controllers;

import com.bob.candidateportal.service.OfferLetterService;
import com.bob.db.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${candidate.api.base.path}/offer-letter")
public class OfferLetterController {

    @Autowired
    private OfferLetterService offerLetterService;

/*    @GetMapping("/get-offer/by-app-id/{applicationId}")
    public ResponseEntity<ApiResponse<?>> getOfferLetter(@PathVariable UUID applicationId){
        return ResponseEntity.ok(ApiResponse.ok(offerLetterService.getOfferLetterDetails(applicationId),"Offer letter details fetched successfully!"));
    }*/

    @GetMapping("/get-by-application-id/{applicationId}")
    public ResponseEntity<ApiResponse<?>> getMyOffers(@PathVariable UUID applicationId){
        return ResponseEntity.ok(ApiResponse.ok(offerLetterService.getMyOffers(applicationId),"Offer letter details fetched successfully!"));
    }

}
