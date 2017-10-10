package com.example.highlite;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.highlite.OnCreate;
import com.jeppeman.highlite.OnOpen;
import com.jeppeman.highlite.OnUpgrade;
import com.jeppeman.highlite.SQLiteDatabaseDescriptor;

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