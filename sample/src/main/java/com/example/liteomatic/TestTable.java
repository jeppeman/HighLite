package com.example.liteomatic;

import com.jeppeman.liteomatic.ForeignKey;
import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "test", autoDeleteColumns = true)
public class TestTable {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteField
    String oldString;

    @SQLiteField(foreignKey = @ForeignKey(table =  MyLittleClass2.class, fieldReference = "id"))
    int foreigners;
}