package com.bob.authapp.controller;

import com.bob.commonutil.config.CaptchaStore;
import com.bob.commonutil.model.CaptchaRequestModel;
import com.bob.commonutil.model.CaptchaResponseModel;
import com.bob.commonutil.service.CaptchaService;
import com.bob.db.dto.ApiResponse;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/generate")
    public ResponseEntity< ApiResponse<CaptchaResponseModel> > getCaptcha() throws Exception {
        CaptchaResponseModel  res = captchaService.generateCaptcha();
        return ResponseEntity.ok(ApiResponse.ok(res,"Captcha generated sucessfully"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verify(@RequestBody CaptchaRequestModel request) {
        if (captchaService.verifyCaptcha(request.getCaptchaId(),request.getCaptchaValue())) {
            return ResponseEntity.ok(ApiResponse.ok("Captcha valid"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.ok("Invalid captcha"));
    }
}
