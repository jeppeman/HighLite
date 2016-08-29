package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated with {@link SQLiteField} will have a corresponding database field
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SQLiteField {
    /**
     * The name of the database field, if left empty the name of the class field will be used
     *
     * @return the name of the field
     */
    String value() default "";

    /**
     * The SQlite type of the field
     *
     * @return the SQLite type of the field
     * @see {@link SQLiteFieldType}
     */
    SQLiteFieldType fieldType() default SQLiteFieldType.UNSPECIFIED;
}
