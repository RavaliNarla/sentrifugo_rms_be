package com.bob.commonutil.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.util.AppConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;

import java.io.IOException;

@Service
@Slf4j
public class CommonMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, AppConstants.UTF_8_ENCODING);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(senderEmail);

            mailSender.send(message);
        } catch (Exception e) {
            throw new CommonException("Failed to send email");
        }
    }

    @Async
    public void sendEmailTempleteFile(String to, String subject, String htmlBody, MultipartFile file){
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, AppConstants.UTF_8_ENCODING);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (file != null && !file.isEmpty()) {
                helper.addAttachment(
                        file.getOriginalFilename(),
                        new ByteArrayResource(file.getBytes())
                );
            }
            helper.addInline("logo", new ClassPathResource("static/images/logo.png"), AppConstants.IMAGE_PNG_TYPE);
            helper.addInline("checkbox", new ClassPathResource("static/images/checkbox.png"), AppConstants.IMAGE_PNG_TYPE);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully");

        } catch (IOException | MessagingException e) {
            throw new CommonException("Failed to send email");
        }
    }
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void sendEmailSync(String to, String subject, String htmlBody, MultipartFile file) throws IOException, MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, AppConstants.UTF_8_ENCODING);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        helper.setFrom(senderEmail);
        helper.addInline("logo", new ClassPathResource("static/images/logo.png"), AppConstants.IMAGE_PNG_TYPE);

        if (file != null && !file.isEmpty()) {
            helper.addAttachment(file.getOriginalFilename(), new ByteArrayResource(file.getBytes()));
        }

        mailSender.send(mimeMessage);
        log.info("Email sent successfully");
    }

}
