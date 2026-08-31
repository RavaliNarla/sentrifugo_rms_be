package com.bob.commonutil.exception;

import java.util.List;

public class ExcelValidationException extends RuntimeException {

    private final List<String> errors;

    public ExcelValidationException(List<String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
