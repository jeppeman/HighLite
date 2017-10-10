package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "table2")
public class TestTable2 {
    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    public long id;

    @SQLiteField
    public TestNonSerializable nonSerializable;
}
