package com.bob.commonutil.exception;

public class MfaRequiredException extends RuntimeException {
    private final String mfaToken;
    public MfaRequiredException(String mfaToken){ super("MFA required"); this.mfaToken = mfaToken;}
    public String getMfaToken(){ return mfaToken; }
}