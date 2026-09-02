package com.sentrifugo.rms.common.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Only instantiated in services that actually configure spring.mail.host (recruiter-portal).
 * auth-portal/master-portal don't send email, so this bean is conditionally absent there.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.name:Sentrifugo RMS}")
    private String appName;

    public void sendHtmlEmail(String to, String subject, String html, byte[] attachmentBytes, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachmentBytes != null, "UTF-8");
            helper.setFrom(fromAddress, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (attachmentBytes != null && attachmentName != null) {
                helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachmentBytes));
            }
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String html) {
        sendHtmlEmail(to, subject, html, null, null);
    }
}
