package com.jeppeman.liteomatic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface SQLiteSetter {
    String value() default "";
    SQLiteFieldType fieldType() default SQLiteFieldType.UNSPECIFIED;
}
