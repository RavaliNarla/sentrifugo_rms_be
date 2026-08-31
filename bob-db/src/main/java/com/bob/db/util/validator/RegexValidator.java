package com.bob.db.util.validator;

import com.bob.db.util.excel.RegexValidate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class RegexValidator implements ConstraintValidator<RegexValidate, String> {

    private Pattern pattern;

    @Override
    public void initialize(RegexValidate annotation) {

        pattern = Pattern.compile(annotation.regex().getRegex());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        // Let @NotNull or @NotBlank handle null/blank if needed
        if (value == null) {
            return true;
        }

        return pattern.matcher(value).matches();
    }
}

