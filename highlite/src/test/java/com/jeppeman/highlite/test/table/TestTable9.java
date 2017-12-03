package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable9 extends TestTable7 {

    @SQLiteColumn
    public String testingNine;
}
