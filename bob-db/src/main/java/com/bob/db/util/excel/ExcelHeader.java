package com.bob.db.util.excel;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelHeader {
    String value();
    boolean isMandatory() default false;
}