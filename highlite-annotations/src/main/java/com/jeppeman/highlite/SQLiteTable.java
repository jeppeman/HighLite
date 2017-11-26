package com.jeppeman.highlite;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate a class with {@link SQLiteTable} to be able to have a database table based on
 * {@link SQLiteTable#tableName()} and the class' enclosed properties annotated with
 * {@link SQLiteField} automatically created.
 *
 *
 * @author jesper
 * @see {@link SQLiteField}
 */
@Retention(RetentionPolicy.CLASS)
public @interface SQLiteTable {

    /**
     * The database that should contain this table, this must be a class annotated with
     * {@link SQLiteDatabaseDescriptor}
     *
     * @return
     */
    Class<?> database();

    /**
     * The name that the database table will have
     *
     * @return a {@link String} representing the name of the table
     */
    String tableName() default "";

    /**
     * Specifies whether the table should be automatically created or not
     *
     * @return true if table should be automatically created, otherwise not
     */
    boolean autoCreate() default true;

    /**
     * Specifies whether the table should have its columns automatically added or not
     *
     * @return true if columns should be automatically added, otherwise false
     */
    boolean autoAddColumns() default true;

    /**
     * Specifies whether the table should have its columns automatically removed or not
     *
     * @return true if colmuns should be automatically removed, otherwise false
     */
    boolean autoDeleteColumns() default false;
}
