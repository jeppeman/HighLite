package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(tableName = "testTable7", database = TestDatabase.class)
public class TestTable7 extends TestTable {

    @SQLiteColumn
    public String testingSeven;
}
