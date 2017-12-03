package com.jeppeman.highlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated with {@link SQLiteColumn} will have a corresponding database field
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SQLiteColumn {
    /**
     * The name of the database field, if left empty the name of the class field will be used
     *
     * @return the name of the field
     */
    String value() default "";

    /**
     * The SQLite type of the field
     *
     * @return the SQLite type of the field
     * @see {@link SQLiteColumnType}
     */
    SQLiteColumnType columnType() default SQLiteColumnType.UNSPECIFIED;

    /**
     * Creates a PRIMARY KEY constraint for the corresponding database column if set to true
     *
     * @return
     */
    PrimaryKey primaryKey() default @PrimaryKey(enabled = false);

    /**
     * Creates a UNIQUE constraint for the corresponding database column if set to true
     *
     * @return
     */
    boolean unique() default false;

    /**
     * Creates a NOT NULL constraint for the corresponding database column if set to true
     *
     * @return the SQLite type of the field
     */
    boolean notNull() default false;

    /**
     * The SQLite type of the field
     *
     * @return
     * @see {@link ForeignKey}
     */
    ForeignKey foreignKey() default @ForeignKey(fieldReference = "", enabled = false);
}
