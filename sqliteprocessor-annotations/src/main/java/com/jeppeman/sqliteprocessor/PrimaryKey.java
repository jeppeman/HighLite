package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated with {@link PrimaryKey} will have the property PRIMARY KEY written
 * to its corresponding database field.
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface PrimaryKey {
    /**
     * Wheather to auto increment this key or not
     *
     * @return true if auto increment otherwise false
     */
    boolean autoIncrement() default false;
}
