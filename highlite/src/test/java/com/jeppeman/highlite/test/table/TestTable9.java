package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable9 extends TestTable7 {

    @SQLiteField
    String testingNine;
}
