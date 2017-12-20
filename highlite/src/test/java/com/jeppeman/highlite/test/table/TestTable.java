package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteRelationship;
import com.jeppeman.highlite.SQLiteTable;

import java.util.Date;
import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(database = TestDatabase.class, autoDeleteColumns = true)
public class TestTable {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    public long id;

    @SQLiteColumn(unique = true)
    public long unique;

    @SQLiteColumn("testFieldName")
    public String testString;

    @SQLiteColumn
    public List<String> testList;

    @SQLiteColumn
    public Boolean testBoolean;

    @SQLiteColumn
    public TestSerializable testSerializable;

    @SQLiteColumn
    public Date testDate;

    @SQLiteColumn
    public int upgradeAddTester;

    @SQLiteRelationship(table = TestTable4.class, backReference = "foreignKey")
    public List<TestTable4> table4Relation;

    @SQLiteRelationship(table = TestTable10.class, backReference = "fk1")
    public List<TestTable10> table10rel1;

    @SQLiteRelationship(table = TestTable10.class, backReference = "fk2")
    public List<TestTable10> table10rel2;
}
