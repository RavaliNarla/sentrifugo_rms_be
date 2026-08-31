package com.bob.commonutil.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.candidateportal.RequestHistoryModel;
import com.bob.commonutil.model.candidateportal.CreateThreadRequestModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ConversationMessagesDTO;
import com.bob.db.dto.ConversationThreadsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.ConversationThreadsStatus;
import com.bob.db.enums.SenderTypeEnum;
import com.bob.db.mapper.ConversationMessagesMapper;
import com.bob.db.mapper.ConversationThreadsMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CandidateThreadService {

    @Autowired
    private ConversationThreadsRepository conversationThreadsRepository;

    @Autowired
    private ConversationMessagesRepository conversationMessagesRepository;

    @Autowired
    private FileService fileService;

    @Value("${candidate.attachment.upload.path}")
    private String CANDIDATE_ATTACHMENT_FOLDER;

    @Value("${candidate.attachment.http.url}")
    private String CANDIDATE_ATTACHMENT_BASE_URL;

    @Autowired
    private ConversationMessagesMapper conversationMessagesMapper;

    @Autowired
    private ConversationThreadsMapper conversationThreadsMapper;

    @Autowired
    private CommonMailService mailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private RequestTypesRepository requestTypeRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    private static final Logger logger = LoggerFactory.getLogger(CandidateThreadService.class);

    public RequestHistoryModel createThread(UUID candidateId,CreateThreadRequestModel createThreadRequestModel, MultipartFile file) throws IOException, MessagingException {


        if (createThreadRequestModel.getApplicationId() == null) {
            throw new IllegalArgumentException("Application ID must not be null");
        }
        ConversationThreadsEntity conversationThreadsEntity= ConversationThreadsEntity.builder()
                .applicationId(createThreadRequestModel.getApplicationId())
                .requestTypeId(createThreadRequestModel.getRequestTypeId())
                .initiatedBy(SenderTypeEnum.CANDIDATE.toString())
                .status(ConversationThreadsStatus.PENDING)
                .dateExtension(createThreadRequestModel.getDateExtension())
                .zonalId(createThreadRequestModel.getZonalId())
                .build();

        ConversationThreadsEntity savedThread= conversationThreadsRepository.save(conversationThreadsEntity);
        String attachmentPath = null;
        if (file != null && !file.isEmpty()) {
            String fileName = DBConstants.REQUEST_ATTACHMENT + "_" + savedThread.getId();
            String path = fileService.uploadFile(file, fileName, CANDIDATE_ATTACHMENT_FOLDER);
            
            // For Azure: CANDIDATE_ATTACHMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_ATTACHMENT_BASE_URL has domain URL
            attachmentPath = (CANDIDATE_ATTACHMENT_BASE_URL != null && !CANDIDATE_ATTACHMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_ATTACHMENT_BASE_URL + "/" + path 
                : CANDIDATE_ATTACHMENT_FOLDER + "/" + path;
        }

        ConversationMessagesEntity conversationMessagesEntity= ConversationMessagesEntity.builder()
                .threadId(savedThread.getId())
                .message(createThreadRequestModel.getDescription())
                .attachmentPath(attachmentPath)
                .senderId(candidateId)
                .senderType(SenderTypeEnum.CANDIDATE)
                .build();

        ConversationMessagesEntity savedMessage= conversationMessagesRepository.save(conversationMessagesEntity);


        mailSenderHelper.sendExtensionApprovalMail(List.of(savedThread.getApplicationId()),null);

        ConversationThreadsDTO conversationThreadsDTO=conversationThreadsMapper.toDTO(savedThread);
        ConversationMessagesDTO conversationMessagesDTO=conversationMessagesMapper.toDto(savedMessage);

        return RequestHistoryModel.builder()
                .conversationThreads(conversationThreadsDTO)
                .conversationMessages(List.of(conversationMessagesDTO))
                .build();

    }

    public List<RequestHistoryModel> getCandidateRequestHistory(List<UUID> applicationId) {
        List<ConversationThreadsEntity> threadsEntities= conversationThreadsRepository.findAllByApplicationIdIn(applicationId);

        threadsEntities.forEach(item ->{
            if( item.getStatus().equals(ConversationThreadsStatus.L1_REJECTED) || item.getStatus().equals(ConversationThreadsStatus.L2_REJECTED) ){
                item.setStatus(ConversationThreadsStatus.REJECTED);
            } else if (item.getStatus().equals(ConversationThreadsStatus.L2_PENDING) || item.getStatus().equals(ConversationThreadsStatus.L1_PENDING)) {
                item.setStatus(ConversationThreadsStatus.PROGRESS);
            }
        });


        List<UUID> threadIds = threadsEntities.stream().map(ConversationThreadsEntity ::getId).toList();
        List<ConversationMessagesEntity> messagesEntities= conversationMessagesRepository.findAllByThreadIdIn(threadIds);
        Map<UUID,List<ConversationMessagesEntity>> messagesListMap = messagesEntities
                .stream().collect(Collectors.groupingBy(ConversationMessagesEntity::getThreadId));

        return threadsEntities.stream().map(thread->{
            return RequestHistoryModel.builder()
                    .conversationThreads(conversationThreadsMapper.toDTO(thread))
                    .conversationMessages(conversationMessagesMapper.toDtoList(messagesListMap.get(thread.getId())))
                    .build();

        }).toList();
    }



}
