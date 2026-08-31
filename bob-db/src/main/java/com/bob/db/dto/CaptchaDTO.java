package com.bob.db.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaptchaDTO {

    private String captchaString;

    private LocalDateTime expiryDate;
}
