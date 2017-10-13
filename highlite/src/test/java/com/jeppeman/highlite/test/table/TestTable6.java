package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable6")
public class TestTable6 {

    public TestTable6() {

    }

    public TestTable6(String primaryString) {
        this.primaryString = primaryString;
    }

    @SQLiteField(primaryKey = @PrimaryKey)
    String primaryString;

    @SQLiteField(notNull = true)
    public String notNullString;
}
