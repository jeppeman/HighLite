package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(tableName = "testTable", autoDeleteColumns = true)
public class TestTable {

    @SQLiteField
    @PrimaryKey(autoIncrement = true)
    public long id;

    @SQLiteField("testFieldName")
    public String testString;

    @SQLiteField
    public List<String> testList;

    @SQLiteField
    public Boolean testBoolean;

    @SQLiteField
    public TestSerializable testSerializable;

    @SQLiteField(isUpgradeAddColumnTest = true)
    public int upgradeAddTester;

    @SQLiteField(isUpgradeDeleteColumnTest = true)
    public String upgradeDeleteTester;
}
