@SQLiteDatabaseHolder(databases = {
        @SQLiteDatabaseDescriptor(
                dbName = "testDatabase",
                dbVersion = 9,
                tables = {
                        MyLittleClass.class,
                        TestTable.class,
                        MyLittleClass2.class
                }
        ),
        @SQLiteDatabaseDescriptor(
                dbName = "bautaDase",
                dbVersion = 1,
                tables = {
                        MyLittleClass3.class
                }
        )
})
package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.SQLiteDatabaseDescriptor;
import com.jeppeman.sqliteprocessor.SQLiteDatabaseHolder;
