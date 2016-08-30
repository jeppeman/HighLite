package com.jeppeman.sqliteprocessor.test.table;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "testTable")
public class TestTable {

    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    public long id;

    @SQLiteField("testFieldName")
    public String testString;

    @SQLiteField
    public List<String> testList;

    @SQLiteField
    public Boolean testBoolean;

    @SQLiteField
    public TestSerializable testSerializable;
}
