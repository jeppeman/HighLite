package com.jeppeman.highlite.test.table;


import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable4")
public class TestTable4 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true), unique = true)
    long id;

    @SQLiteField(unique = true)
    public String uniqueField;

    @SQLiteField(foreignKey = @ForeignKey(
            fieldReference = "id",
            cascadeOnUpdate = true,
            cascadeOnDelete = true))
    public TestTable foreignKey;
}
