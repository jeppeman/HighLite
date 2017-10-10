package com.example.highlite;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteFieldType;
import com.jeppeman.highlite.SQLiteTable;

import java.util.List;

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
