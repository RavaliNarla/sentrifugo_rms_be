package com.bob.db.util.excel;



import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelDropdown {
    Class<?> masterClass();   // Entity class
    String displayField();    // Display field in entity
    ExcelFilter[] filters() default {};//Filters for displaying only some data
}
