package com.example.highlite;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = BautaDase.class, tableName = "test2")
public class TestTable2 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;
}