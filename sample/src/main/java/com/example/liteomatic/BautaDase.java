package com.example.liteomatic;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.liteomatic.OnCreate;
import com.jeppeman.liteomatic.OnOpen;
import com.jeppeman.liteomatic.OnUpgrade;
import com.jeppeman.liteomatic.SQLiteDatabaseDescriptor;

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
