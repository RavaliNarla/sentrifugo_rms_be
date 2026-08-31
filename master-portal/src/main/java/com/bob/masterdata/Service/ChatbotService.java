package com.bob.masterdata.Service;

import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.ChatbotCategoryEntity;
import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.entity.MasterPositionsEntity;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.repository.CandidateApplicationsRepository;
import com.bob.db.repository.ChatbotCategoryRepository;
import com.bob.db.repository.MasterPositionsRepository;
import com.bob.db.repository.PositionsRepository;
import com.bob.commonutil.util.AppConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class ChatbotService {

    @Autowired
    private ChatbotCategoryRepository chatbotCategoryRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    record AppliedJobs(UUID positionID, String positionTitle, CandidateApplicationStatus applicationStatus){};
    record CurrentJobs(String positionTitle , Integer mandatoryExperience, String mandatoryQualification){}

    public String getChatFAQReply(String question) {
        try {
            if (question == null || question.trim().isEmpty()) {
                List<String> parentCategories = chatbotCategoryRepository.findDistinctParentCategories();
                return objectMapper.writeValueAsString(parentCategories);
            }

            // 1. Check parent_category
            List<ChatbotCategoryEntity> parentCategoryMatches = chatbotCategoryRepository.findByParentCategory(question);
            if (!parentCategoryMatches.isEmpty()) {
                List<String> categoryNames = parentCategoryMatches.stream()
                        .map(ChatbotCategoryEntity::getCategoryName)
                        .distinct()
                        .collect(Collectors.toList());
                return objectMapper.writeValueAsString(categoryNames);
            }

            // 2. Check category_name
            List<ChatbotCategoryEntity> categoryNameMatches = chatbotCategoryRepository.findByCategoryName(question);
            if (!categoryNameMatches.isEmpty()) {
                List<String> questions = categoryNameMatches.stream()
                        .map(ChatbotCategoryEntity::getQuestion)
                        .distinct()
                        .collect(Collectors.toList());
                return objectMapper.writeValueAsString(questions);
            }

            // 3. Check question
            List<ChatbotCategoryEntity> questionMatches = chatbotCategoryRepository.findByQuestion(question);
            if (!questionMatches.isEmpty()) {
                List<String> answers = questionMatches.stream()
                        .map(ChatbotCategoryEntity::getAnswer)
                        .collect(Collectors.toList());
                return objectMapper.writeValueAsString(answers);
            }

            // No results
            return AppConstants.CHAT_BOT_CONTACT_SUPPORT;
        } catch (JsonProcessingException e) {
            // Consider logging the exception
            return "Error processing your request";
        }
    }



    public String getChatQueryReply(String question, UUID candidateId){
        try {
            if (question.equals(AppConstants.APPLICATION_STATUS)) {
                List<CandidateApplicationsEntity> latest5 =
                        candidateApplicationsRepository.findTop5ByCandidateIdOrderByCreatedDateDesc(candidateId);
                List<UUID> posIds = latest5.stream()
                        .map(pos -> pos.getPositionId())
                        .toList();
                List<JobPositionsEntity> positionsEntities = positionsRepository.findAllById(posIds);
                List<UUID> masterPosIds = positionsEntities.stream()
                        .map(pos -> pos.getMasterPositionId())
                        .toList();


                Map<UUID, UUID> posNameMap = positionsEntities.stream()
                        .collect(Collectors.toMap(
                                JobPositionsEntity::getId,
                                JobPositionsEntity::getMasterPositionId
                        ));

                Map<UUID, String> masterPosNameMap = masterPositionsRepository.findAllById(masterPosIds).stream()
                        .collect(Collectors.toMap(
                                mpos -> mpos.getId(),
                                mpos -> mpos.getPositionName()
                        ));

                // Step 5: Convert each application to AppliedJobs record
                List<AppliedJobs> appliedJobsList = latest5.stream()
                        .map(job -> new AppliedJobs(
                                job.getPositionId(),
                                masterPosNameMap.get(posNameMap.get(job.getPositionId())),
                                job.getApplicationStatus()
                        ))
                        .toList();

                return objectMapper.writeValueAsString(appliedJobsList);
            }

            if (question.equals(AppConstants.CURRENT_JOB_OPPORTUNITIES)) {
                List<JobPositionsEntity> latestJobs = positionsRepository.findTop5ByOrderByCreatedDateDesc();

                List<UUID> masterPosIds = latestJobs.stream()
                        .map(pos -> pos.getMasterPositionId())
                        .toList();
                List<MasterPositionsEntity> masterPositions = masterPositionsRepository.findAllById(masterPosIds);
                Map<UUID, String> masterPosNameMap = masterPositions.stream()
                        .collect(Collectors.toMap(
                                mpos -> mpos.getId(),
                                mpos -> mpos.getPositionName()
                        ));
                List<CurrentJobs> positionsEntities = latestJobs.stream().map(job ->
                        new CurrentJobs(masterPosNameMap.get(job.getMasterPositionId()), job.getMandatoryExperienceMonths(), job.getMandatoryEducation())).toList();
                return  objectMapper.writeValueAsString(positionsEntities);
            }

            return AppConstants.CHAT_BOT_CONTACT_SUPPORT;
        } catch (JsonProcessingException e) {
            
            return "Error processing your request";
        }
    }
}
