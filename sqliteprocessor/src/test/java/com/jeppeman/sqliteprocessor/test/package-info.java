@SQLiteDatabaseHolder(databases = {
        @SQLiteDatabaseDescriptor(
                dbName = "testDatabase",
                dbVersion = 1,
                tables = {
                        TestTable.class,
                        TestTable2.class,
                        TestTable3.class
                }
        )
})
package com.jeppeman.sqliteprocessor.test;

import com.jeppeman.sqliteprocessor.SQLiteDatabaseDescriptor;
import com.jeppeman.sqliteprocessor.SQLiteDatabaseHolder;
import com.jeppeman.sqliteprocessor.test.table.TestTable;
import com.jeppeman.sqliteprocessor.test.table.TestTable2;
import com.jeppeman.sqliteprocessor.test.table.TestTable3;
