package com.bob.jobportal.controller;

import com.bob.commonutil.model.candidateportal.RequestHistoryModel;
import com.bob.db.dto.ConversationThreadsDTO;
import com.bob.jobportal.model.MessageApprovalRequestModel;
import com.bob.jobportal.model.MessageThreadRequestModel;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.bob.db.dto.ApiResponse;
import com.bob.jobportal.model.MessageResponseModel;
import com.bob.jobportal.model.MessageThreadResponseModel;
import com.bob.jobportal.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${recruiter.api.base.path}/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/get-history")
    public ResponseEntity<ApiResponse<Page<MessageThreadResponseModel>>> getAllConversationHistory(
            @Valid @RequestBody MessageThreadRequestModel request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<MessageThreadResponseModel> reponse = messageService.getAllConversationHistory(request,pageable);
        return ResponseEntity.ok(ApiResponse.ok(reponse, "Messages fetched successfully"));
    }

    @GetMapping("/get-message/{conversationThreadId}")
    public ResponseEntity<ApiResponse<List<MessageResponseModel>>> getAllMessages(@PathVariable UUID conversationThreadId){
        List<MessageResponseModel> response = messageService.getAllMessages(conversationThreadId);
        return ResponseEntity.ok(ApiResponse.ok(response,"Messages fetched successfully"));
    }

    @PostMapping("/submit-for-l1-l2-approval")
    public ResponseEntity<ApiResponse<List<ConversationThreadsDTO>>> submitForApprovalL1AndL2(@RequestBody MessageApprovalRequestModel approvalRequestModel) throws MessagingException, IOException {
        List<ConversationThreadsDTO> response = messageService.submitForApprovalL1AndL2(approvalRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(response,"Successfully updated"));
    }

    @PostMapping("/submit-for-approval")
    public ResponseEntity<ApiResponse<RequestHistoryModel>> submitForApproval(@RequestBody MessageApprovalRequestModel approvalRequestModel) throws MessagingException, IOException {
         RequestHistoryModel response = messageService.submitForApproval(approvalRequestModel);
        return ResponseEntity.ok(ApiResponse.ok(response,"Successfully updated"));
    }



    @PostMapping("/get-approvals")
    public ResponseEntity<ApiResponse<Page<MessageThreadResponseModel>>> getAllConversationApprovalsBasedOnApproverRoles(
            @RequestBody MessageThreadRequestModel request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){

        Page<MessageThreadResponseModel> reponse = messageService.getConversationHistoryBasedOnApproverRoles(request,page,size);
        return ResponseEntity.ok(ApiResponse.ok(reponse, "Messages fetched successfully"));
    }

}
