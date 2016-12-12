package com.example.liteomatic;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.liteomatic.OnCreate;
import com.jeppeman.liteomatic.OnOpen;
import com.jeppeman.liteomatic.OnUpgrade;
import com.jeppeman.liteomatic.SQLiteDatabaseDescriptor;

@SQLiteDatabaseDescriptor(
        dbName = "testDatabase",
        dbVersion = 11
)
public class TestDatabase {

    private TestDatabase() {

    }

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