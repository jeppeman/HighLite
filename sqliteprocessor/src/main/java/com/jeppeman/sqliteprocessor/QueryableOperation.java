package com.jeppeman.sqliteprocessor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("unchecked")
public abstract class QueryableOperation<T extends QueryableOperation> {

    String mRawQueryClause;
    Object[] mRawQueryArgs;
    SQLiteQuery mQuery;

    public T withRawQuery(final @NonNull String rawQueryClause,
                          final @Nullable Object... rawQueryArgs) {
        mQuery = null;
        mRawQueryClause = rawQueryClause;
        mRawQueryArgs = rawQueryArgs;
        return (T) this;
    }

    public T withQuery(final @NonNull SQLiteQuery query) {
        mRawQueryClause = null;
        mRawQueryArgs = null;
        mQuery = query;
        return (T) this;
    }
}
