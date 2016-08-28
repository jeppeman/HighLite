package com.jeppeman.sqliteprocessor;

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
public class SQLiteQuery {

    String mGroupByClause;
    String mHavingClause;
    String mOrderByClause;
    String mWhereClause;
    Object[] mWhereArgs;

    SQLiteQuery() {

    }

    public static Builder builder() {
        return new Builder(new SQLiteQuery());
    }

    public static class Builder {

        private SQLiteQuery mQuery;

        Builder(final @NonNull SQLiteQuery query) {
            mQuery = query;
        }

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

        public SQLiteQuery build() {
            return mQuery;
        }
    }
}
