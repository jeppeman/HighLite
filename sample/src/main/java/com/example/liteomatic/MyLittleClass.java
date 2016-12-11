package com.example.liteomatic;

import com.jeppeman.liteomatic.ForeignKey;
import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteFieldType;
import com.jeppeman.liteomatic.SQLiteTable;

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