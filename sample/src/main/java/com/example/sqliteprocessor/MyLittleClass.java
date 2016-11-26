package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.ForeignKey;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "myLittleTable")
public class MyLittleClass {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;

    @SQLiteField
    String name;

    @SQLiteField
    @ForeignKey(
            table = "test",
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true)
    int foreign;

    @SQLiteField
    @ForeignKey(
            table = "test2",
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true)
    int foreign2;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    Short shortz;

    @SQLiteField("someOtherNameeps")
    List<String> nameList;

    @SQLiteField
    Boolean yo;

    @SQLiteField
    String newString;

    @SQLiteField
    String newerString;
}