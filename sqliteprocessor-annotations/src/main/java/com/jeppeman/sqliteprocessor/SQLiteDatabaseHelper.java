package com.jeppeman.sqliteprocessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jesper on 2016-08-26.
 */
@Retention(RetentionPolicy.CLASS)
public @interface SQLiteDatabaseHelper {
    String name ();
    int version();
}
