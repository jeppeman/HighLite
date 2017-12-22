package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable13 {
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    public int id;

    @SQLiteColumn(foreignKey = @ForeignKey(fieldReference = "id"))
    public TestTable12 tttt;
}
