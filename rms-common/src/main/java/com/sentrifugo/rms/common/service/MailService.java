package com.sentrifugo.rms.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Candidate messages go out through the configured SMTP server.
 * Every other message is stored in Mailpit and is not delivered to the address in To.
 * Temporary while RMS is exercised against real employee inboxes. Restore real
 * delivery by sending the non-candidate path through {@code mailSender} again.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.name:Sentrifugo RMS}")
    private String appName;

    @Value("${app.mailpit.base-url:http://172.16.2.111:8125}")
    private String mailpitBaseUrl;

    public MailService(@Qualifier("mailSender") JavaMailSender mailSender, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

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
        deliver(to, subject, html, attachments, false);
    }

    public void sendCandidateHtmlEmail(String to, String subject, String html, List<Attachment> attachments) {
        deliver(to, subject, html, attachments, true);
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

    @Async
    public void sendCandidateHtmlEmailAsync(String to, String subject, String html, List<Attachment> attachments) {
        sendCandidateHtmlEmail(to, subject, html, attachments);
    }

    private void deliver(String to, String subject, String html, List<Attachment> attachments, boolean candidate) {
        try {
            if (candidate) {
                sendViaSmtp(to, subject, html, attachments);
            } else {
                sendViaMailpit(to, subject, html, attachments);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String html, List<Attachment> attachments) throws Exception {
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
    }

    /**
     * Mailpit's send API writes the message into its own mailbox. It does not relay to the recipient.
     */
    private void sendViaMailpit(String to, String subject, String html, List<Attachment> attachments) throws Exception {
        String base = mailpitBaseUrl == null ? "" : mailpitBaseUrl.replaceAll("/+$", "");
        if (base.isBlank()) {
            log.error("Mailpit URL is blank; refused to deliver user email to {}", to);
            return;
        }
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode from = body.putObject("From");
        from.put("Email", fromAddress == null || fromAddress.isBlank() ? "noreply@sentrifugo.local" : fromAddress);
        from.put("Name", appName);
        ObjectNode recipient = body.putArray("To").addObject();
        recipient.put("Email", to);
        body.put("Subject", subject == null ? "" : subject);
        body.put("HTML", html == null ? "" : html);
        body.putArray("Tags").add("rms");
        if (attachments != null && !attachments.isEmpty()) {
            ArrayNode files = body.putArray("Attachments");
            for (Attachment attachment : attachments) {
                if (attachment == null || attachment.bytes() == null || attachment.fileName() == null) {
                    continue;
                }
                ObjectNode file = files.addObject();
                file.put("Filename", attachment.fileName());
                if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
                    file.put("ContentType", attachment.contentType());
                }
                file.put("Content", Base64.getEncoder().encodeToString(attachment.bytes()));
            }
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/v1/send"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = response.body() == null ? "" : response.body();
            if (detail.length() > 300) {
                detail = detail.substring(0, 300);
            }
            throw new IllegalStateException("Mailpit HTTP " + response.statusCode() + " " + detail);
        }
        log.info("Stored user email in Mailpit instead of delivering it to {}", to);
    }
}
