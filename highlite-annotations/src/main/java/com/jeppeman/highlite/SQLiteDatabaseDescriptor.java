package com.jeppeman.highlite;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a database that is going to be used in an application in terms of name
 * ({@link SQLiteDatabaseDescriptor#dbName()}), and version
 * ({@link SQLiteDatabaseDescriptor#dbVersion()})
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
}
