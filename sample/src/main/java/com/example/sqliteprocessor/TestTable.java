package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

/**
 * Created by jesper on 2016-10-09.
 */

@SQLiteTable(tableName = "test")
public class TestTable {

    @SQLiteField
    @PrimaryKey
    int id;
}
