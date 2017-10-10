package com.jeppeman.highlite;

import android.support.annotation.NonNull;

/**
 * Subclasses of this class have the ability to specify {@link SQLiteQuery}s that are used for their
 * corresponding database operations.
 *
 * @param <T> a class extending this class.
 * @author jesper
 */
@SuppressWarnings("unchecked")
public abstract class QueryableOperation<T extends QueryableOperation> {

    SQLiteQuery mQuery;

    QueryableOperation() {

    }

    /**
     * Attaches an {@link SQLiteQuery} to the operating subclass.
     *
     * @param query the {@link SQLiteQuery} to attach.
     * @return itself
     */
    public T withQuery(final @NonNull SQLiteQuery query) {
        mQuery = query;
        return (T) this;
    }
}