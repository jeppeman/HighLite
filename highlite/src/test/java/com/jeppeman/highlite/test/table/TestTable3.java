package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "testTable3", autoCreate = false)
public class TestTable3 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long xx;

    @SQLiteField(notNull = true)
    public String str;
}
