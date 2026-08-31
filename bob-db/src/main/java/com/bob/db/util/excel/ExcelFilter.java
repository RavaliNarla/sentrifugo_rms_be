package com.bob.db.util.excel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

@Target(ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelFilter {

    String field();                // e.g. "committee.id"

    Operator operator() default Operator.EQUAL;

    String param();                // ExcelFilterContext key

    LogicalOperator logical() default LogicalOperator.AND;
}
