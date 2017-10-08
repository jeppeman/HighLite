package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

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
