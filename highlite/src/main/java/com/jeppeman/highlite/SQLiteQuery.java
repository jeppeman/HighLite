package com.jeppeman.highlite;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Query builder for SQLite queries designed to not have to deal with the annoying null argument
 * passing to the {@link android.database.sqlite.SQLiteDatabase#query(boolean, String, String[],
 * String, String[], String, String, String, String)} method.
 *
 * @author jesper
 */
@SuppressWarnings("unused")
public final class SQLiteQuery {

    String mLimitClause;
    String mGroupByClause;
    String mHavingClause;
    String mOrderByClause;
    String mWhereClause;
    Object[] mWhereArgs;

    private SQLiteQuery() {

    }

    public static Builder builder() {
        return new Builder(new SQLiteQuery());
    }

    public static final class Builder {

        private SQLiteQuery mQuery;

        Builder(final @NonNull SQLiteQuery query) {
            mQuery = query;
        }

        /**
         * The where clause for this query.
         * Example: "field = ? AND timeStamp > ?"
         *
         * @param whereClause the where clause where parameters are given as ?
         * @param whereArgs the parameter values for the where clause where each parameter
         *                  corresponds to a ? in the clause
         * @return
         */
        public Builder where(final @NonNull String whereClause,
                             final @Nullable Object... whereArgs) {
            mQuery.mWhereClause = whereClause;
            mQuery.mWhereArgs = whereArgs;
            return this;
        }

        public Builder orderBy(final @NonNull String orderByClause) {
            mQuery.mOrderByClause = orderByClause;
            return this;
        }

        public Builder groupBy(final @NonNull String groupByClause) {
            mQuery.mGroupByClause = groupByClause;
            return this;
        }

        public Builder having(final @NonNull String havingClause) {
            mQuery.mHavingClause = havingClause;
            return this;
        }

        public Builder limit(final @NonNull String limitClause) {
            mQuery.mLimitClause = limitClause;
            return this;
        }

        public SQLiteQuery build() {
            return mQuery;
        }
    }
}