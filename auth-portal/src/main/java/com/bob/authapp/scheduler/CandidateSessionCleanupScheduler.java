package com.bob.authapp.scheduler;

import com.bob.authapp.service.CandidateSessionManagementService;
import com.bob.commonutil.service.CaptchaService;
import com.bob.db.repository.CaptchaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@Slf4j
public class CandidateSessionCleanupScheduler {

    @Autowired
    private CandidateSessionManagementService candidateSessionManagementService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * Scheduled task to clean up expired candidate sessions
     * Runs every 30 minutes
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void cleanupExpiredCandidateSessions() {
        log.info("Starting scheduled cleanup of expired candidate sessions");
        int deletedCount = candidateSessionManagementService.cleanupExpiredSessions();
        log.info("Scheduled cleanup completed. Deleted {} expired candidate sessions", deletedCount);

        log.info("Starting expired captcha cleanup");
        int deletedCaptchaCount = captchaService.deleteExpiredCaptchas();
        log.info("Expired captcha cleanup completed.Deleted count: {}",deletedCaptchaCount);
    }


}
