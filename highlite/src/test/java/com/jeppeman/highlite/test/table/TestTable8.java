package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable8 extends TimestampedModel {

    @SQLiteColumn(primaryKey = @PrimaryKey)
    public long id;

    @SQLiteColumn
    public String testingEight;
}
