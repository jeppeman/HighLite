package com.jeppeman.sqliteprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A package annotated with {@link SQLiteDatabaseHolder} has the ability to have the tables from
 * {@link SQLiteDatabaseDescriptor#tables()} automatically created, its columns automatically
 * added and dropped if opted in.
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface SQLiteDatabaseHolder {
    /**
     * The databases contained in this package
     *
     * @return an array of {@link SQLiteDatabaseDescriptor}s
     * @see {@link SQLiteDatabaseDescriptor}
     */
    SQLiteDatabaseDescriptor[] databases();
}
