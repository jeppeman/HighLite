package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jesper on 2016-08-25.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface SQLiteGetter {
    String value() default "";
    SQLiteFieldType fieldType() default SQLiteFieldType.UNSPECIFIED;
}
