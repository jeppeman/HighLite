package com.example.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;

import com.jeppeman.sqliteprocessor.SQLiteDatabaseHelper;
import com.jeppeman.sqliteprocessor.SQLiteProcessorHelper;

/**
 * Created by jesper on 2016-08-26.
 */
@SQLiteDatabaseHelper(name = "bautaDasen", version = 1)
public class DBHelper extends SQLiteProcessorHelper {

    protected DBHelper(@NonNull Context context, @NonNull String name, int version) {
        super(context, name, version);
    }
}
