package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable6")
public class TestTable6 {

    public TestTable6() {

    }

    public TestTable6(String primaryString) {
        this.primaryString = primaryString;
    }

    @SQLiteColumn(primaryKey = @PrimaryKey)
    String primaryString;

    @SQLiteColumn(notNull = true)
    public String notNullString;
}
