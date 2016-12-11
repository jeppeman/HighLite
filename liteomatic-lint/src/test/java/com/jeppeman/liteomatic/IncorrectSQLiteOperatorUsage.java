package com.jeppeman.liteomatic;

import android.content.Context;

import org.mockito.Mockito;

public class IncorrectSQLiteOperatorUsage {

    void dumb() {
        SQLiteOperator.from(Mockito.mock(Context.class), String.class);
    }

    void dumber() {
        dumb();
    }
}
