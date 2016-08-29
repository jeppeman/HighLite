@SQLiteDatabaseHolder(databases = {
        @SQLiteDatabaseDescriptor(
                dbName = "testDatabase",
                dbVersion = 12,
                tables = {
                        MyLittleClass.class,
                        MyLittleClass2.class
                }
        ),
        @SQLiteDatabaseDescriptor(
                dbName = "bautaDase",
                dbVersion = 3,
                tables = {
                        MyLittleClass3.class
                }
        )
})
package com.example.sqliteprocessor;

import com.jeppeman.sqliteprocessor.SQLiteDatabaseDescriptor;
import com.jeppeman.sqliteprocessor.SQLiteDatabaseHolder;
