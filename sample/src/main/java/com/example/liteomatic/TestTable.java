package com.example.liteomatic;

import com.jeppeman.liteomatic.ForeignKey;
import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

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