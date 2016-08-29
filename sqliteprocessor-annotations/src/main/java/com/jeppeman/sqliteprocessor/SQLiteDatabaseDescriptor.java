package com.jeppeman.sqliteprocessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a database that is going to be used in an application in terms of name
 * ({@link SQLiteDatabaseDescriptor#dbName()}), version
 * ({@link SQLiteDatabaseDescriptor#dbVersion()}) and tables
 * ({@link SQLiteDatabaseDescriptor#tables()}). The tables must be classes annotated with
 * {@link SQLiteTable}.
 *
 * @author jesper
 */
@Retention(RetentionPolicy.CLASS)
public @interface SQLiteDatabaseDescriptor {
    /**
     * The name of the database
     *
     * @return the name of the database
     */
    String dbName();

    /**
     * The version of the database
     *
     * @return the version of the database
     */
    int dbVersion();

    /**
     * The tables of the database represented as {@link Class}es, these must be annotated with
     * {@link SQLiteTable}
     *
     * @return
     */
    Class<?>[] tables();
}
