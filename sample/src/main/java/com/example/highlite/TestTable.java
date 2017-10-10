package com.example.highlite;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class, tableName = "test", autoDeleteColumns = true)
public class TestTable {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteField
    String oldString;

    @SQLiteField(foreignKey = @ForeignKey(table =  MyLittleClass2.class, fieldReference = "id"))
    int foreigners;
}