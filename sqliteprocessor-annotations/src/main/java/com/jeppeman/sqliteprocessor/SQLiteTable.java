package com.jeppeman.sqliteprocessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jesper on 2016-08-24.
 */
@Retention(RetentionPolicy.CLASS)
public @interface SQLiteTable {
    String tableName();
    boolean autoCreate() default true;
    boolean autoAddColumns() default true;
    boolean autoDeleteColumns() default false;
}
