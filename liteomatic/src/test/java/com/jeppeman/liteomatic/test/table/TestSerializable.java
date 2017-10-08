package com.jeppeman.liteomatic.test.table;

import java.io.Serializable;

public class TestSerializable implements Serializable {
    public static final long serialVersionUid = 5L;

    public String testField;

    public TestSerializable(String testField) {
        this.testField = testField;
    }
}
