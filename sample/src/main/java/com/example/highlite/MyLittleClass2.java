package com.example.highlite;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteFieldType;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(
        database = TestDatabase.class,
        tableName = "myLittleTable2",
        autoDeleteColumns = true,
        autoAddColumns = false)
public class MyLittleClass2 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteField
    String namezz;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    float shortz;

    @SQLiteField(fieldType = SQLiteFieldType.INTEGER)
    boolean testBool;
}