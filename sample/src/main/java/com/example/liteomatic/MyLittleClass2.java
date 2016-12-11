package com.example.liteomatic;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteFieldType;
import com.jeppeman.liteomatic.SQLiteTable;

@SQLiteTable(
        tableName = "myLittleTable2",
        autoDeleteColumns = true,
        autoAddColumns = false)
public class MyLittleClass2 {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;

    @SQLiteField
    String namezz;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    short shortz;

    @SQLiteField
    boolean testBool;
}