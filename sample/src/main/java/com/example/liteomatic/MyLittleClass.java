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