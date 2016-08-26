package com.jeppeman.sqliteprocessor;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jesper on 2016-08-26.
 */
public interface SQLiteOpenHelperCallbacks {
    void onCreate(SQLiteDatabase database);
    void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion);
}
