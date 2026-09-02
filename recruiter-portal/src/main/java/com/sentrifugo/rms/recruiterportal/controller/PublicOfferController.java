package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.recruiterportal.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Unauthenticated endpoints hit directly from the Accept/Reject buttons in the offer
 * email. Not part of the Azure AD security chain - see PUBLIC_ENDPOINTS allowlist.
 */
@Tag(name = "Public - Offer Decision")
@RestController
@RequestMapping("/api/v1/public/offers")
@RequiredArgsConstructor
public class PublicOfferController {

    private final OfferService offerService;

    @Operation(summary = "Candidate clicks Accept in the offer email")
    @GetMapping("/{token}/accept")
    public ResponseEntity<String> accept(@PathVariable UUID token) {
        return renderResult(offerService.decide(token, true));
    }

    @Operation(summary = "Candidate clicks Reject in the offer email")
    @GetMapping("/{token}/reject")
    public ResponseEntity<String> reject(@PathVariable UUID token) {
        return renderResult(offerService.decide(token, false));
    }

    private ResponseEntity<String> renderResult(String result) {
        String title;
        String message;
        String color;
        switch (result) {
            case "ACCEPTED" -> {
                title = "Offer Accepted";
                message = "Thank you! Your acceptance has been recorded successfully.";
                color = "#208bbd";
            }
            case "REJECTED" -> {
                title = "Offer Declined";
                message = "Your response has been recorded. We wish you the best in your future endeavours.";
                color = "#a20e37";
            }
            case "EXPIRED" -> {
                title = "Link Expired";
                message = "The accept-before date for this offer has passed and this link is no longer valid.";
                color = "#888888";
            }
            default -> {
                title = "Invalid Link";
                message = "This offer link is invalid or no longer exists.";
                color = "#888888";
            }
        }

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>" + title + "</title>"
                + "<style>body{font-family:Arial,Helvetica,sans-serif;background:#e7ebec;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}"
                + ".card{background:#fff;padding:48px 56px;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,0.1);text-align:center;max-width:420px;}"
                + ".tick{font-size:48px;color:" + color + ";}"
                + "h1{color:" + color + ";font-size:22px;margin:16px 0 8px;}"
                + "p{color:#555;font-size:14px;}</style></head>"
                + "<body><div class='card'><div class='tick'>&#10003;</div><h1>" + title + "</h1><p>" + message + "</p></div></body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
