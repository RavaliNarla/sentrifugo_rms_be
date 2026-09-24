package com.sentrifugo.rms.recruiterportal.controller;

import com.sentrifugo.rms.recruiterportal.service.InterviewSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Unauthenticated Accept / Decline endpoints from the interview invite email
 * (same pattern as PublicOfferController).
 */
@Tag(name = "Public - Interview Invite Decision")
@RestController
@RequestMapping("/api/v1/public/interviews")
@RequiredArgsConstructor
public class PublicInterviewController {

    private final InterviewSchedulingService interviewSchedulingService;

    @Operation(summary = "Candidate clicks Accept in the interview invite email")
    @GetMapping("/{token}/accept")
    public ResponseEntity<String> accept(@PathVariable UUID token) {
        return renderResult(interviewSchedulingService.decideInvite(token, true));
    }

    @Operation(summary = "Candidate clicks Decline in the interview invite email")
    @GetMapping("/{token}/decline")
    public ResponseEntity<String> decline(@PathVariable UUID token) {
        return renderResult(interviewSchedulingService.decideInvite(token, false));
    }

    private ResponseEntity<String> renderResult(String result) {
        String title;
        String message;
        String color;
        switch (result) {
            case "ACCEPTED" -> {
                title = "Interview Accepted";
                message = "Thank you! Your acceptance has been recorded. We look forward to meeting you.";
                color = "#208bbd";
            }
            case "DECLINED" -> {
                title = "Interview Declined";
                message = "Your response has been recorded. Thank you for letting us know.";
                color = "#a20e37";
            }
            case "ALREADY_ACCEPTED" -> {
                title = "Already Accepted";
                message = "You have already accepted this interview invitation.";
                color = "#208bbd";
            }
            case "ALREADY_DECLINED" -> {
                title = "Already Declined";
                message = "You have already declined this interview invitation.";
                color = "#a20e37";
            }
            case "SUPERSEDED" -> {
                title = "Link No Longer Valid";
                message = "A newer interview invite has been sent. Please use the Accept / Decline links in the most recent email.";
                color = "#888888";
            }
            default -> {
                title = "Invalid Link";
                message = "This interview invite link is invalid or no longer exists.";
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
