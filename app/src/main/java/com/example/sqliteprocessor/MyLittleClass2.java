package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteObject;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

@SQLiteTable(
        tableName = "myLittleTable2",
        autoDeleteColumns = true,
        autoAddColumns = false,
        autoCreate = false)
public class MyLittleClass2 extends SQLiteObject {

    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    int id;

    @SQLiteField
    String namezz;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    short shortz;

    @SQLiteField
    List<String> nameList;
}
