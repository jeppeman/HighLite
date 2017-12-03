package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "testTable3", autoCreate = false)
public class TestTable3 {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long xx;

    @SQLiteColumn(notNull = true)
    public String str;

    @SQLiteColumn(unique = true)
    public String unique;

    @SQLiteColumn(foreignKey = @ForeignKey(fieldReference = "id"))
    public TestTable foreign;
}
