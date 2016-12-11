package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "table2")
public class TestTable2 {
    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    public long id;

    @SQLiteField
    public TestNonSerializable nonSerializable;
}
