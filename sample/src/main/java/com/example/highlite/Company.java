package com.example.highlite;

import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteRelationship;
import com.jeppeman.highlite.SQLiteTable;

import java.util.List;

/**
 * @author jesper
 */
@SQLiteTable(database = CompanyDatabase.class)
public class Company extends TimestampedModel {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteColumn need to be at least package local

    @SQLiteColumn("companyName")
    String name;

    @SQLiteRelationship(backReference = "company")
    List<Employee> employees;
}