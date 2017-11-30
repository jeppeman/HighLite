package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteField;

import java.util.Date;

public class TimestampedModel {

    @SQLiteField
    public Date created;

    @SQLiteField
    public Date modified;
}
