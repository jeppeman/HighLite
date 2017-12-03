package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteColumn;

import java.util.Date;

public class TimestampedModel {

    @SQLiteColumn
    public Date created;

    @SQLiteColumn
    public Date modified;
}
