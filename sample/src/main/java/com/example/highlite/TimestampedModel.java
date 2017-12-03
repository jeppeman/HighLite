package com.example.highlite;

import com.jeppeman.highlite.SQLiteColumn;

import java.util.Date;

public class TimestampedModel {

    @SQLiteColumn
    Date created;

    @SQLiteColumn
    Date modified;
}
