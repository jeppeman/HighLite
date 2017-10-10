package com.example.highlite;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.highlite.OnCreate;
import com.jeppeman.highlite.OnOpen;
import com.jeppeman.highlite.OnUpgrade;
import com.jeppeman.highlite.SQLiteDatabaseDescriptor;

@SQLiteDatabaseDescriptor(
        dbName = "bautaDase",
        dbVersion = 1
)
public class BautaDase {

    private BautaDase() {

    }

    @OnOpen
    public static void onOpen(SQLiteDatabase database) {
        int x = 5;
    }

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {
        int x = 2;
    }

    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }

}