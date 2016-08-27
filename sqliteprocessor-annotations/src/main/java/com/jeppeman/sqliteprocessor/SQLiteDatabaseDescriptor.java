package com.jeppeman.sqliteprocessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jesper on 2016-08-27.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface SQLiteDatabaseDescriptor {
    String dbName();
    int dbVersion();
    Class<?>[] tables();
}
