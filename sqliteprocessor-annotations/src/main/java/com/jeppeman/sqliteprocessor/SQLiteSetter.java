package com.jeppeman.sqliteprocessor;

/**
 * Created by jesper on 2016-08-25.
 */
public @interface SQLiteSetter {
    String value() default "";
    SQLiteFieldType fieldType() default SQLiteFieldType.UNSPECIFIED;
}
