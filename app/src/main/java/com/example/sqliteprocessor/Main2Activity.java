package com.example.sqliteprocessor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.jeppeman.sqliteprocessor.SQLiteTable;

@SQLiteTable(databaseName = "DB", tableName = "TABLE", version = 0)
public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }
}
