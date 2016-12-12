package com.example.liteomatic;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

@SQLiteTable(database = BautaDase.class, tableName = "test2")
public class TestTable2 {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    int id;
}