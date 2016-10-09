package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.ForeignKey;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

/**
 * Created by jesper on 2016-10-09.
 */

@SQLiteTable(tableName = "test", autoDeleteColumns = true)
public class TestTable {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;

    @SQLiteField
    String oldString;

    @SQLiteField
    @ForeignKey(table = "myLittleTable2", fieldReference = "id")
    int foreigners;
}