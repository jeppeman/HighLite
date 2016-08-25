package com.jeppeman.sqliteprocessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jesper on 2016-08-24.
 */
@Retention(RetentionPolicy.CLASS)
public @interface SQLiteTable {
    String value();
    String databaseName();
    int version();
}
