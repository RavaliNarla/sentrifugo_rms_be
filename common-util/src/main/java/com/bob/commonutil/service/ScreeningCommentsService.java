package com.bob.commonutil.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.CreateScreeningCommentRequestModel;
import com.bob.commonutil.model.ScreeningCommentResponseModel;
import com.bob.commonutil.model.ScreeningCommentsResponseModel;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.ScreeningCommentsEntity;
import com.bob.db.enums.ScreeningCommentUserRole;
import com.bob.db.enums.UserRole;
import com.bob.db.repository.CandidateApplicationsRepository;
import com.bob.db.repository.ScreeningCommentsRepository;
import com.bob.db.util.DBConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ScreeningCommentsService {

    @Autowired
    private ScreeningCommentsRepository screeningCommentsRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public ScreeningCommentsResponseModel getComments(UUID applicationId) {
        UUID currentUserId = getCurrentUserIdOrThrow();
        CandidateApplicationsEntity application = getApplicationOrThrow(applicationId);
        ScreeningCommentUserRole userRole = resolveCurrentUserRole();

        validateReadAccess(userRole, currentUserId, application);

        List<ScreeningCommentResponseModel> comments = screeningCommentsRepository
                .findAllByApplicationId(applicationId, Sort.by(
                        Sort.Order.asc("createdDate"),
                        Sort.Order.asc("id")
                ))
                .stream()
                .map(this::toResponseModel)
                .toList();

        return ScreeningCommentsResponseModel.builder()
                .applicationId(applicationId)
                .comments(comments)
                .build();
    }

    public ScreeningCommentsResponseModel addComment(UUID applicationId, CreateScreeningCommentRequestModel request) {
        UUID currentUserId = getCurrentUserIdOrThrow();
        CandidateApplicationsEntity application = getApplicationOrThrow(applicationId);
        ScreeningCommentUserRole userRole = resolveCurrentUserRole();

        validateReadAccess(userRole, currentUserId, application);

        String trimmedComment = request.getCommentText().trim();
        if (trimmedComment.isEmpty()) {
            throw new IllegalArgumentException("Comment text is required");
        }

        ScreeningCommentsEntity savedEntity = screeningCommentsRepository.save(
                ScreeningCommentsEntity.builder()
                        .applicationId(applicationId)
                        .candidateId(application.getCandidateId())
                        .userId(currentUserId)
                        .userRole(userRole)
                        .commentText(trimmedComment)
                        .build()
        );

        return ScreeningCommentsResponseModel.builder()
                .applicationId(applicationId)
                .comments(List.of(toResponseModel(savedEntity)))
                .build();
    }

    private ScreeningCommentUserRole resolveCurrentUserRole() {
        String client = securityUtils.getClient();
        if (client != null && client.equalsIgnoreCase(DBConstants.HEADER_CANDIDATE)) {
            return ScreeningCommentUserRole.CANDIDATE;
        }

        String currentRole = securityUtils.getCurrentUserRole();
        UserRole userRole = UserRole.fromValue(currentRole);
        if (userRole == UserRole.RECRUITER) {
            return ScreeningCommentUserRole.RECRUITER;
        }

        throw new AccessDeniedException("Only candidate and recruiter can access screening comments");
    }

    private void validateReadAccess(
            ScreeningCommentUserRole role,
            UUID currentUserId,
            CandidateApplicationsEntity application
    ) {
        if (role == ScreeningCommentUserRole.CANDIDATE
                && !application.getCandidateId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to access this application comments");
        }
    }

    private CandidateApplicationsEntity getApplicationOrThrow(UUID applicationId) {
        return candidateApplicationsRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private UUID getCurrentUserIdOrThrow() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Unable to resolve current user");
        }
        return currentUserId;
    }

    private ScreeningCommentResponseModel toResponseModel(ScreeningCommentsEntity entity) {
        return ScreeningCommentResponseModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userRole(entity.getUserRole())
                .commentText(entity.getCommentText())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}

