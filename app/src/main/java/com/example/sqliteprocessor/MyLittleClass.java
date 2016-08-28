package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteObject;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * @az
 */
@SQLiteTable(tableName = "myLittleTable", autoDeleteColumns = true)
public class MyLittleClass extends SQLiteObject {

    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    int id;

    @SQLiteField
    String name;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    Short shortz;

    @SQLiteField("someOtherNameeps")
    List<String> nameList;

    @SQLiteField
    Boolean yo;

//    @SQLiteGetter
//    @PrimaryKey
//    @AutoIncrement
//    public int getId() {
//        return id;
//    }
//
//    @SQLiteSetter
//    @PrimaryKey
//    @AutoIncrement
//    public void setId(final int id) {
//        this.id = id;
//    }
}
