package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: Not sure if I'm going to implement this or not..
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface SQLiteGetter {
    String value() default "";
    SQLiteFieldType fieldType() default SQLiteFieldType.UNSPECIFIED;
}
