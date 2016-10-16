package com.example.sqliteprocessor;

import android.database.sqlite.SQLiteDatabase;

import com.jeppeman.sqliteprocessor.OnCreate;
import com.jeppeman.sqliteprocessor.OnOpen;
import com.jeppeman.sqliteprocessor.OnUpgrade;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

@SQLiteTable(tableName = "test2")
public class TestTable2 {

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {

    }

    @OnUpgrade
    public static void onCreate(SQLiteDatabase database, int oldVersion, int newVersion) {
        int x = 1;
    }

    @OnOpen
    public static void onOpen(SQLiteDatabase database) {

    }

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;
}