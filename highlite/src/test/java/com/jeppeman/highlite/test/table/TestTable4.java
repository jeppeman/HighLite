package com.jeppeman.highlite.test.table;


import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable4")
public class TestTable4 {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true), unique = true)
    long id;

    @SQLiteColumn(unique = true)
    public String uniqueField;

    @SQLiteColumn(foreignKey = @ForeignKey(
            fieldReference = "id",
            cascadeOnUpdate = true,
            cascadeOnDelete = true))
    public TestTable foreignKey;
}
