package com.sentrifugo.rms.common.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public record Attachment(String fileName, byte[] bytes, String contentType) {
        public Attachment(String fileName, byte[] bytes) {
            this(fileName, bytes, null);
        }
    }

    public void sendHtmlEmail(String to, String subject, String html, byte[] attachmentBytes, String attachmentName) {
        List<Attachment> attachments = null;
        if (attachmentBytes != null && attachmentName != null) {
            attachments = List.of(new Attachment(attachmentName, attachmentBytes));
        }
        sendHtmlEmail(to, subject, html, attachments);
    }

    public void sendHtmlEmail(String to, String subject, String html) {
        sendHtmlEmail(to, subject, html, (List<Attachment>) null);
    }

    public void sendHtmlEmail(String to, String subject, String html, List<Attachment> attachments) {
        try {
            boolean multipart = attachments != null && !attachments.isEmpty();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(fromAddress, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (multipart) {
                for (Attachment attachment : attachments) {
                    if (attachment == null || attachment.bytes() == null || attachment.fileName() == null) {
                        continue;
                    }
                    ByteArrayResource resource = new ByteArrayResource(attachment.bytes()) {
                        @Override
                        public String getFilename() {
                            return attachment.fileName();
                        }
                    };
                    if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
                        helper.addAttachment(attachment.fileName(), resource, attachment.contentType());
                    } else {
                        helper.addAttachment(attachment.fileName(), resource);
                    }
                }
            }
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    /** Fire-and-forget so APIs are not blocked on SMTP. */
    @Async
    public void sendHtmlEmailAsync(String to, String subject, String html) {
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendHtmlEmailAsync(String to, String subject, String html, List<Attachment> attachments) {
        sendHtmlEmail(to, subject, html, attachments);
    }
}
