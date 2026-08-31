package com.bob.commonutil.model;

import com.bob.commonutil.enums.SmsTemplateType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsRequest {
    private String phNumber;
    private SmsTemplateType templateName;
    private String postingName;
    private String applicationId;
    private String regNo;
    private String otp;
}
