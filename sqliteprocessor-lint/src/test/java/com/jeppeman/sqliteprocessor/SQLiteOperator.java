package com.jeppeman.sqliteprocessor;

import android.content.Context;

import org.mockito.Mockito;

public class SQLiteOperator {

    public static SQLiteOperator from(final Context context, final Class<?> cls) {
        return new SQLiteOperator();
    }

    void dummy() {
        from(Mockito.mock(Context.class), TestTable.class);
    }
}
