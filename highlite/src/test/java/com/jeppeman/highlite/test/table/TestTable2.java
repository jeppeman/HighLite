package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class)
public class TestTable2 {
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    public long id;

    @SQLiteColumn
    public TestNonSerializable nonSerializable;

    @SQLiteColumn
    public String testString;

    @SQLiteColumn
    public String testStringLonger;
}
