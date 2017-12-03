package com.example.highlite;

import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = CompanyDatabase.class)
public class Developer extends Employee {

    @SQLiteField
    String type;
}
