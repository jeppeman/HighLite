package com.example.sqliteprocessor;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.sqliteprocessor.OnCreate;
import com.jeppeman.sqliteprocessor.OnOpen;
import com.jeppeman.sqliteprocessor.OnUpgrade;
import com.jeppeman.sqliteprocessor.SQLiteDatabaseDescriptor;

/**
 * Created by jesper on 2016-11-26.
 */

@SQLiteDatabaseDescriptor(
        dbName = "testDatabase",
        dbVersion = 11,
        tables = {
                MyLittleClass.class,
                TestTable.class,
                MyLittleClass2.class,
                TestTable2.class
        })
public class TestDatabase {

    @OnOpen
    public static void onOpen(SQLiteDatabase database) {

    }

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {
        int x = 3;
    }

    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }
}
