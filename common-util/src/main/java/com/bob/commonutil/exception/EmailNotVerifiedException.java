package com.bob.commonutil.exception;

public class EmailNotVerifiedException extends RuntimeException {
    private final String userId;
    public EmailNotVerifiedException(String userId){ super("Email not verified"); this.userId = userId;}
    public String getUserId(){ return userId; }
}