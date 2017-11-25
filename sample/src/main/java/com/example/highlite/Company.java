package com.example.highlite;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteField;
import com.jeppeman.highlite.SQLiteTable;

import java.util.Date;
import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(database = CompanyDatabase.class, tableName = "myLittleTable")
public class Company {

    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteField need to be at least package local

    @SQLiteField("companyName")
    String name;

    @SQLiteField
    Date created; // Dates are stored as INTEGER's with the amount of seconds since UNIX epoch

    @SQLiteField
    List<String> employees; // This will get saved as a BLOB in the database
}