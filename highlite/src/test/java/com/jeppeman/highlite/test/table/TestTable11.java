package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;

abstract class TestTable11 {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    public int id;

    @SQLiteColumn(foreignKey = @ForeignKey(fieldReference = "id"))
    public TestTable tttt;
}
