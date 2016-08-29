package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated with {@link AutoIncrement} will have the property AUTOINCREMENT written
 * to it's corresponding database field. Will only have an effect in it is annotated on a field
 * which is also annotated with {@link PrimaryKey}.
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AutoIncrement {
}
