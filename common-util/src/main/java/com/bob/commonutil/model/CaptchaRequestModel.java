package com.bob.commonutil.model;

import lombok.Data;

import java.util.UUID;

@Data
public class CaptchaRequestModel {
    private UUID captchaId;
    private String captchaValue;
}
