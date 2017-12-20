package com.jeppeman.highlite.test.table;

import com.jeppeman.highlite.ForeignKey;
import com.jeppeman.highlite.PrimaryKey;
import com.jeppeman.highlite.SQLiteColumn;
import com.jeppeman.highlite.SQLiteTable;

@SQLiteTable(database = TestDatabase.class)
public class TestTable10 {
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    int id;

    @SQLiteColumn(foreignKey = @ForeignKey(
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true
    ))
    public TestTable fk1;

    @SQLiteColumn(foreignKey = @ForeignKey(
            fieldReference = "id",
            cascadeOnDelete = true,
            cascadeOnUpdate = true
    ))
    public TestTable fk2;
}
