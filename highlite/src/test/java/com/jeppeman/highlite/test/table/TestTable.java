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

    @SQLiteColumn
    public TestEnum testEnum;

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

    @SQLiteRelationship(backReference = "foreignKey")
    public List<TestTable4> table4Relation;

    @SQLiteRelationship(backReference = "fk1")
    public List<TestTable10> table10rel1;

    @SQLiteRelationship(backReference = "fk2")
    public TestTable10 table10rel2;
}
