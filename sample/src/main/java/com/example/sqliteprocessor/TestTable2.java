package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

@SQLiteTable(tableName = "test2")
public class TestTable2 {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;
}