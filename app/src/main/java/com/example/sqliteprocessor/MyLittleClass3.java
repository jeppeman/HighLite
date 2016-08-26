package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteObject;
import com.jeppeman.sqliteprocessor.SQLiteTable;

import java.util.List;

/**
 * Created by jesper on 2016-08-25.
 */
@SQLiteTable(tableName = "myLittleTable2", sqLiteHelper = DBHelper.class)
public class MyLittleClass3 extends SQLiteObject {

    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    int id;

    @SQLiteField
    String name;

    @SQLiteField(fieldType = SQLiteFieldType.REAL)
    short shortz;

    @SQLiteField
    List<String> nameList;
}
