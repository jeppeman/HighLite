package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable8 extends TimestampedModel {

    @SQLiteField(primaryKey = @PrimaryKey)
    long id;

    @SQLiteField
    String testingEight;
}
