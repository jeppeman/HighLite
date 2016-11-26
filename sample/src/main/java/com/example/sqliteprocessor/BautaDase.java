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
        dbName = "bautaDase",
        dbVersion = 1,
        tables = {
                MyLittleClass3.class
        }
)
public class BautaDase {

    @OnOpen
    public static void onOpen(SQLiteDatabase database) {
        int x = 2;
    }

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {
        int x = 2;
    }

    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }

}
