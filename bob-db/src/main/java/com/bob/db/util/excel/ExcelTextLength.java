package com.bob.db.util.excel;
import com.bob.db.util.DBConstants;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelTextLength {

    int min() default DBConstants.EXCEL_MIN_CELL_LENGTH;
    int max() default DBConstants.EXCEL_MAX_CELL_LENGTH;
}
