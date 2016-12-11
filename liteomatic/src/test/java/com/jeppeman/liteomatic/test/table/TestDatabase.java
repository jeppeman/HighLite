package com.jeppeman.liteomatic.test.table;

import com.jeppeman.liteomatic.SQLiteDatabaseDescriptor;

@SQLiteDatabaseDescriptor(
        dbName = "testDatabase",
        dbVersion = 1,
        tables = {
                TestTable.class,
                TestTable2.class,
                TestTable3.class,
                TestTable4.class,
                TestTable5.class
        }
)
public class TestDatabase {
}
