package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "testTable3", autoCreate = false)
public class TestTable3 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long xx;

    @SQLiteField
    String str;
}
