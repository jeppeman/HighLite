package com.jeppeman.sqliteprocessor.test.table;

import java.io.Serializable;

/**
 * Created by jesper on 2016-08-29.
 */
public class TestSerializable implements Serializable {
    public static final long serialVersionUid = 5L;

    public String testField;

    public TestSerializable(String testField) {
        this.testField = testField;
    }
}
