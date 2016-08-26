package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * Created by jesper on 2016-08-25.
 */
@SQLiteTable(tableName = "myLittleTable", databaseName = "myLittleDatabase", version = 17)
public class MyLittleClass {

    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    int id;

    @SQLiteField
    String name;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    short shortz;

    @SQLiteField
    List<String> nameList;
}
