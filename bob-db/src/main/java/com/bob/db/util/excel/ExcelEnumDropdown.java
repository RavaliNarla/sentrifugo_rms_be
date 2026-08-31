package com.bob.db.util.excel;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelEnumDropdown {

    Class<? extends Enum<?>> enumClass();

    String displayField() default "";
}