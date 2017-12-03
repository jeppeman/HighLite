package com.example.highlite;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(
        database = CompanyDatabase.class,
        tableName = "employees"
)
public class Employee {

    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteColumn need to be package local

    @SQLiteColumn("employeeName")
    String name;

    @SQLiteColumn
    float salary;

    @SQLiteColumn(foreignKey = @ForeignKey(
            fieldReference = "id", // Note: this is the name of the field of the class you are
            // referring to, not the database column name; the field has to be unique
            cascadeOnDelete = true, // defaults to false
            cascadeOnUpdate = true // defaults to false
    ))
    Company company;
}