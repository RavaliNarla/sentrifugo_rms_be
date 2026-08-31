package com.bob.db.util.excel;
import com.bob.db.enums.RegexPattern;
import com.bob.db.util.validator.RegexValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RegexValidator.class)
public @interface RegexValidate {
    RegexPattern regex();
    String message() default "Invalid format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
