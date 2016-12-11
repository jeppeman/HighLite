package com.jeppeman.liteomatic;

import android.content.Context;

import org.mockito.Mockito;

public class CorrectSQLiteOperatorUsage {

    void dumb() {
        SQLiteOperator.from(Mockito.mock(Context.class), TestTable.class);
    }

    void dumber() {
        dumb();
    }
}
