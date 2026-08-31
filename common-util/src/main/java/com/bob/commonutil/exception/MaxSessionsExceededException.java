package com.bob.commonutil.exception;

import lombok.Getter;

@Getter
public class MaxSessionsExceededException extends RuntimeException {
    private final int maxSessions;
    private final String email;

    public MaxSessionsExceededException(String email, int maxSessions) {
        super(String.format("Maximum concurrent sessions (%d) reached for email: %s. Please logout from another device first.", maxSessions, email));
        this.email = email;
        this.maxSessions = maxSessions;
    }
}
