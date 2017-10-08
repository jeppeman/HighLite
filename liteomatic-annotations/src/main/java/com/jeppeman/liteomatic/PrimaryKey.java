package com.jeppeman.liteomatic;

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
@Target(ElementType.ANNOTATION_TYPE)
public @interface PrimaryKey {
    /**
     * Weather to auto increment this key or not
     *
     * @return true if auto increment otherwise false
     */
    boolean autoIncrement() default false;

    boolean enabled() default true;
}
