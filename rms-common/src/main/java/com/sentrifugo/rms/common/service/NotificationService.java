package com.sentrifugo.rms.common.service;

import com.sentrifugo.rms.db.entity.NotificationEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.UserRole;
import com.sentrifugo.rms.db.repository.NotificationRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-app (+ email) notifications. Rows stay in DB forever; UI only shows the last 3 days.
 * DB writes happen in the request transaction; emails are queued after commit (async).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_REQUISITION_SUBMITTED = "REQUISITION_SUBMITTED";
    public static final String TYPE_OFFER_SUBMITTED = "OFFER_SUBMITTED";
    public static final String TYPE_INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    public static final int VISIBLE_DAYS = 3;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<MailService> mailService;

    public LocalDateTime visibilityCutoff() {
        return LocalDateTime.now().minusDays(VISIBLE_DAYS);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> listForUser(UUID userId) {
        return notificationRepository.findByUserIdAndCreatedDateGreaterThanEqualOrderByCreatedDateDesc(
                userId, visibilityCutoff());
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNullAndCreatedDateGreaterThanEqual(
                userId, visibilityCutoff());
    }

    @Transactional
    public void markAllVisibleAsRead(UUID userId) {
        notificationRepository.markUnreadAsRead(userId, visibilityCutoff(), LocalDateTime.now());
    }

    /** Fan-out to every Admin and Recruiter (not committee). Emails go out after commit, async. */
    @Transactional
    public void notifyAdminsAndRecruiters(String type, String message) {
        List<UserEntity> recipients = userRepository.findByRoleIn(List.of(
                UserRole.ADMIN.getValue(),
                UserRole.RECRUITER.getValue()));
        if (recipients.isEmpty()) {
            return;
        }

        List<NotificationEntity> rows = new ArrayList<>(recipients.size());
        List<PendingEmail> emails = new ArrayList<>(recipients.size());
        String subject = emailSubject(type);
        for (UserEntity user : recipients) {
            rows.add(NotificationEntity.builder()
                    .userId(user.getId())
                    .type(type)
                    .message(message)
                    .interviewCount(0)
                    .build());
            emails.add(new PendingEmail(user.getEmail(), user.getName(), subject, message));
        }
        notificationRepository.saveAll(rows);
        queueEmailsAfterCommit(emails);
    }

    /**
     * One notification per committee member per interview date. Re-schedules the same day
     * refresh the count/message and re-mark unread.
     */
    @Transactional
    public void upsertCommitteeInterviewDay(UUID userId, LocalDate interviewDate, int dayInterviewCount) {
        upsertCommitteeInterviewDay(userId, interviewDate, dayInterviewCount, true);
    }

    /**
     * @param sendEmail when false, only the in-app bell row is updated (caller may send a richer email with ICS).
     */
    @Transactional
    public void upsertCommitteeInterviewDay(UUID userId, LocalDate interviewDate, int dayInterviewCount, boolean sendEmail) {
        if (userId == null || interviewDate == null) {
            return;
        }
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        // Count 0 after declines: clear/update the row so the bell matches reality.
        String message = interviewMessage(interviewDate, Math.max(dayInterviewCount, 0));
        NotificationEntity existing = notificationRepository
                .findByUserIdAndTypeAndReferenceDate(userId, TYPE_INTERVIEW_SCHEDULED, interviewDate)
                .orElse(null);

        if (dayInterviewCount <= 0) {
            if (existing != null) {
                existing.setInterviewCount(0);
                existing.setMessage(message);
                notificationRepository.save(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setInterviewCount(dayInterviewCount);
            existing.setMessage(message);
            existing.setReadAt(null);
            notificationRepository.save(existing);
        } else {
            notificationRepository.save(NotificationEntity.builder()
                    .userId(user.getId())
                    .type(TYPE_INTERVIEW_SCHEDULED)
                    .message(message)
                    .referenceDate(interviewDate)
                    .interviewCount(dayInterviewCount)
                    .build());
        }
        if (sendEmail) {
            queueEmailsAfterCommit(List.of(
                    new PendingEmail(user.getEmail(), user.getName(), "Interview scheduled", message)));
        }
    }

    private static String interviewMessage(LocalDate interviewDate, int count) {
        String day = interviewDate.format(DATE_FMT);
        if (count <= 0) {
            return "No active interviews remaining on " + day + ".";
        }
        return "You have " + count + " interview" + (count == 1 ? "" : "s")
                + " scheduled on " + day + ".";
    }

    private static String emailSubject(String type) {
        return switch (type) {
            case TYPE_REQUISITION_SUBMITTED -> "New requisition submitted for approval";
            case TYPE_OFFER_SUBMITTED -> "New offer letter submitted for approval";
            case TYPE_INTERVIEW_SCHEDULED -> "Interview scheduled";
            default -> "Sagar Recruitment Hub notification";
        };
    }

    private void queueEmailsAfterCommit(List<PendingEmail> emails) {
        if (emails == null || emails.isEmpty()) {
            return;
        }
        Runnable send = () -> {
            MailService mail = mailService.getIfAvailable();
            if (mail == null) {
                return;
            }
            for (PendingEmail email : emails) {
                if (email.to() == null || email.to().isBlank()) {
                    continue;
                }
                String html = "<p>Hi " + escape(email.name()) + ",</p>"
                        + "<p>" + escape(email.message()) + "</p>"
                        + "<p>Please log in to Sagar Recruitment Hub for details.</p>";
                mail.sendHtmlEmailAsync(email.to(), email.subject(), html);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record PendingEmail(String to, String name, String subject, String message) {
    }
}
