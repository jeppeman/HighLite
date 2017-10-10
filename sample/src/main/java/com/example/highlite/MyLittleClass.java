package com.example.highlite;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteFieldType;
import com.jeppeman.highlite.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "myLittleTable")
public abstract class MyLittleClass {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteField
    String name;

    @SQLiteField(foreignKey = @ForeignKey(
            table = TestTable.class,
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true))
    int foreign;

    @SQLiteField(foreignKey = @ForeignKey(
            table = TestTable.class,
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true))
    int foreign2;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    float shortz;

    @SQLiteField("someOtherNameeps")
    List<String> nameList;

    @SQLiteField
    Boolean yo;

    @SQLiteField
    String newString;

    @SQLiteField
    String newerString;
}