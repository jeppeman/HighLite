package com.jeppeman.highlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated with {@link ForeignKey} will have the property FOREIGN KEY written
 * to its corresponding database field.
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ForeignKey {

    /**
     * The table in which the foreign key resides
     *
     * @return
     */
    Class<?> table();

    /**
     * The name of the field which this key references
     *
     * @return
     */
    String fieldReference();

    /**
     * @return
     */
    boolean cascadeOnDelete() default false;

    /**
     * @return
     */
    boolean cascadeOnUpdate() default false;

    boolean enabled() default true;
}
