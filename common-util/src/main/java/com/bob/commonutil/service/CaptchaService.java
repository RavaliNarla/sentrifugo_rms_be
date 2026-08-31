package com.bob.commonutil.service;

import com.bob.commonutil.config.CaptchaStore;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.CaptchaResponseModel;
import com.bob.db.entity.CaptchaEntity;
import com.bob.db.repository.CaptchaRepository;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Service
public class CaptchaService {


    @Autowired
    private DefaultKaptcha kaptcha;

    @Autowired
    private CaptchaRepository captchaRepository;

    /**
     * Verifies the captcha provided in the request against the stored value.
     *
     * @param captchaId The unique identifier of the captcha
     * @param captchaValue The text entered by the user
     * @return true if valid, false otherwise
     */
    @Transactional
    public boolean verifyCaptcha(UUID captchaId, String captchaValue) {
        if (captchaId == null || captchaValue == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();

        CaptchaEntity captchaEntity = captchaRepository.findById(captchaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid captcha.Please generate a new CAPTCHA and try again."));



        if (captchaEntity.getExpiryDate().isBefore(now)) {
            throw new ManualValidationException("Captcha expired. Please generate a new CAPTCHA and try again.");
        }

        boolean isValidCaptcha = Objects.equals(captchaEntity.getCaptchaString(), captchaValue);

        if(isValidCaptcha){
           captchaRepository.deleteById(captchaId);
        }

        return isValidCaptcha;
    }

    @Transactional
    public CaptchaResponseModel generateCaptcha() throws IOException {
        String text = kaptcha.createText();
        BufferedImage image = kaptcha.createImage(text);



        CaptchaEntity captchaEntity = CaptchaEntity.builder()
                .captchaString(text)
                .expiryDate(LocalDateTime.now().plusMinutes(2))
                .build();

        captchaRepository.save(captchaEntity);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);

        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return CaptchaResponseModel.builder().image(base64).captchaId(captchaEntity.getId()).build();
    }


    @Transactional
    public int deleteExpiredCaptchas() {
        LocalDateTime now = LocalDateTime.now();
        return captchaRepository.deleteByExpiryDateBefore(now);
    }



}
