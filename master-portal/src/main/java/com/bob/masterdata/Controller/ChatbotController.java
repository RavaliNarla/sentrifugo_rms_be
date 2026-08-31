package com.bob.masterdata.Controller;

import com.bob.db.dto.ApiResponse;
import com.bob.masterdata.Service.ChatbotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${master.api.base.path}/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping("/getChatFAQReply")
    public ResponseEntity<ApiResponse<String>> getChatFAQReply(@RequestParam(value = "question", required = false, defaultValue = "") String question) {
        String reply = chatbotService.getChatFAQReply(question);
        ApiResponse<String> response = new ApiResponse<>(true, "DATA FETCHED SUCCESSFULLY!", reply);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/getChatQueryReply")
    public ResponseEntity<ApiResponse<String>> getChatQueryReply(@RequestParam(value = "question", required = false, defaultValue = "") String question,@RequestParam(value = "candidateId", required = true) UUID candidateId) {
        String reply = chatbotService.getChatQueryReply(question, candidateId);
        ApiResponse<String> response = new ApiResponse<>(true, "DATA FETCHED SUCCESSFULLY!", reply);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
