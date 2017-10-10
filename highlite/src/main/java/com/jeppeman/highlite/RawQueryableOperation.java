package com.jeppeman.highlite;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Subclasses of this class have the ability to specify raw queries that are used for their
 * corresponding database operations.
 *
 * @param <T> a class extending this class.
 * @author jesper
 */
public abstract class RawQueryableOperation<T extends QueryableOperation>
        extends QueryableOperation<T> {

    String mRawQueryClause;
    Object[] mRawQueryArgs;

    RawQueryableOperation() {

    }

    /**
     * Attaches a raw query to the operating subclass. Parameters are specified as '?' in the query.
     * Example: SELECT * FROM someTable where `someInt` > ?.
     *
     * @param rawQueryClause the raw query clause.
     * @param rawQueryArgs the parameter values for the clause, each corresponding to a ? in the
     *                     clause.
     * @return itself
     */
    public T withRawQuery(final @NonNull String rawQueryClause,
                          final @Nullable Object... rawQueryArgs) {
        mQuery = null;
        mRawQueryClause = rawQueryClause;
        mRawQueryArgs = rawQueryArgs;
        return (T) this;
    }

    /**
     * @see {@link QueryableOperation#withQuery(SQLiteQuery)}
     */
    @Override
    public T withQuery(@NonNull SQLiteQuery query) {
        mRawQueryClause = null;
        mRawQueryArgs = null;
        return super.withQuery(query);
    }
}