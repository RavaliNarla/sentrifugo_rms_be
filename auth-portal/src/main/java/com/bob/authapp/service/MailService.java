package com.bob.authapp.service;

import com.bob.authapp.utils.AppConstants;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.entity.CandidatesEntity;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;

    @Value("${spring.mail.username}")
    private String senderEmail;


    @Async
    public void sendVerificationEmail(String recipientEmail, String fullName, String token) {

        String verificationUrl = verificationBaseUrl + "?token=" + token;

        Context context = new Context();
        context.setVariable("candidate_name", fullName);
        context.setVariable("verification_link", verificationUrl);

        String htmlBody = templateEngine.process("email-verification", context);

        sendHtmlEmail(recipientEmail, "Verify Your Email Account", htmlBody);
    }


    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(senderEmail);
            helper.addInline("logo", new ClassPathResource("static/images/logo.png"), "image/png");

            mailSender.send(message);
            log.info("Mail sent successfully");

        } catch (Exception e) {
            throw new CommonException("Failed to send email");
        }
    }


    @Async
    public void sendMobileOTP(String mobileNumber, String email, String name, String otp) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);

            String html = templateEngine.process(AppConstants.EMAIL_OTP_CONFIRMATION_TEMPLATE, context);

            sendHtmlEmail(email, "Your One Time Password (OTP) ", html);

        } catch (Exception e) {
            throw new CommonException("Failed to send OTP confirmation email. Please try again.");
        }
    }


    @Async
    public void sendSuccessFullRegistrationMail(CandidatesEntity candidate){
        try{
            Context context = new Context();
            context.setVariable("fullName",candidate.getFullName());
            context.setVariable("email",candidate.getEmail());
            context.setVariable("mobileNumber",commonUtilityProvider.maskMobileNumber(candidate.getMobileNumber()));
            String html = templateEngine.process("registration-successful",context);

            sendHtmlEmail(candidate.getEmail(),"Registration Successful",html);

        } catch (Exception e) {
            throw new CommonException("Failed to send successful registration mail.Please try again");
        }

    }

}
