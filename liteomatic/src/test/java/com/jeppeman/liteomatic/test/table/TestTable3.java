package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "testTable3", autoCreate = false)
public class TestTable3 {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    public long xx;
}
