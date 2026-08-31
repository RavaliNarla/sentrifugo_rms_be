package com.bob.commonutil.exception;

import lombok.Data;

@Data
public class SchedulingConflictException extends RuntimeException{
    private final String message;
    private final Object response;

    public SchedulingConflictException(String message, Object response) {
        this.message = message;
        this.response = response;
    }
}
