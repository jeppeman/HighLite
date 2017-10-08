package com.jeppeman.liteomatic.test.table;


import com.jeppeman.liteomatic.ForeignKey;
import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "testTable4")
public class TestTable4 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true), unique = true)
    long id;

    @SQLiteField(unique = true)
    public String uniqueField;

    @SQLiteField(foreignKey = @ForeignKey(
            table = TestTable.class,
            fieldReference = "id",
            cascadeOnUpdate = true,
            cascadeOnDelete = true))
    public long foreignKey;
}
