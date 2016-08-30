package com.jeppeman.sqliteprocessor.test.table;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "table2")
public class TestTable2 {
    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    public long id;

    @SQLiteField
    public TestNonSerializable nonSerializable;
}
