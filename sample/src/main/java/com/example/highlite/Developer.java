package com.example.highlite;

import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = CompanyDatabase.class)
public class Developer extends Employee {

    @SQLiteColumn
    String type;
}
