package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteField;

import java.util.Date;

public class TimestampedModel {

    @SQLiteField
    Date created;

    @SQLiteField
    Date modified;
}
