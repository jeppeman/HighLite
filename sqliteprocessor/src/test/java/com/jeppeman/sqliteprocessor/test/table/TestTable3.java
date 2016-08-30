package com.jeppeman.sqliteprocessor.test.table;

import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "testTable3", autoCreate = false)
public class TestTable3 {

    @PrimaryKey
    public String xx;
}
