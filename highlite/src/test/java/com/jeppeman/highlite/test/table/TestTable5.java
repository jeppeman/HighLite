package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable5")
public class TestTable5 {

    public TestTable5() {

    }

    public TestTable5(String primaryString) {
        this.primaryString = primaryString;
    }

    @SQLiteField(primaryKey = @PrimaryKey)
    String primaryString;
}
