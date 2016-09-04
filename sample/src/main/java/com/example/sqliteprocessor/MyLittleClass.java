package com.example.sqliteprocessor;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.OnCreate;
import com.jeppeman.sqliteprocessor.OnUpgrade;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "myLittleTable", autoCreate = false, autoAddColumns = false)
public class MyLittleClass {

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {

    }

    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase database, int oldVersionz, int newVersion) {

    }

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
