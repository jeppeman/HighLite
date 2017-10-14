package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteRelationship;
import com.jeppeman.highlite.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, tableName = "testTable", autoDeleteColumns = true)
public class TestTable {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    public long id;

    @SQLiteField("testFieldName")
    public String testString;

    @SQLiteField
    public List<String> testList;

    @SQLiteField
    public Boolean testBoolean;

    @SQLiteField
    public TestSerializable testSerializable;

    @SQLiteField
    public int upgradeAddTester;

    @SQLiteRelationship(table = TestTable4.class)
    public List<TestTable4> table4Relation;
}
