package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(tableName = "testTable7", database = TestDatabase.class)
public class TestTable7 extends TestTable {

    @SQLiteField
    String testingSeven;
}