package com.bob.commonutil.enums;


public enum SmsTemplateType {
    JOBAPPLY("JOBAPPLY"),
    REGISTRATION("REGISTRATION"),
    OTP("OTP");
    
    private final String value;
    
    SmsTemplateType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
