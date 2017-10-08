package com.example.liteomatic;

import com.jeppeman.liteomatic.PrimaryKey;
import com.jeppeman.liteomatic.SQLiteField;
import com.jeppeman.liteomatic.SQLiteFieldType;
import com.jeppeman.liteomatic.SQLiteTable;

import java.util.List;

/**
 * Created by jesper on 2016-08-25.
 */
@SQLiteTable(database = BautaDase.class, tableName = "myLittleTable2")
public class MyLittleClass3 {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteField
    String name;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    short shortz;

    @SQLiteField
    List<Integer> nameList;
}
