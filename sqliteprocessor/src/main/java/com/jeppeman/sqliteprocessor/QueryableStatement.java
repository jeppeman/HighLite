package com.jeppeman.sqliteprocessor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class QueryableStatement extends Statement {

    String mRawQueryClause;
    Object[] mRawQueryArgs;
    SQLiteQuery mQuery;

    public QueryableStatement withRawQuery(final @NonNull String rawQueryClause,
                                              final @Nullable Object... rawQueryArgs) {
        mQuery = null;
        mRawQueryClause = rawQueryClause;
        mRawQueryArgs = rawQueryArgs;
        return this;
    }

    public QueryableStatement withQuery(final @NonNull SQLiteQuery query) {
        mRawQueryClause = null;
        mRawQueryArgs = null;
        mQuery = query;
        return this;
    }
}
