package com.jeppeman.highlite;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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

    private void fixNonStringParameters() {
        final String[] split = mQuery.mWhereClause.split("\\?");
        final List<String> whereArgsList = new ArrayList<>(), paramReplacements = new ArrayList<>();

        for (int i = 0; i < mQuery.mWhereArgs.length; i++) {
            if (String.class.equals(mQuery.mWhereArgs[i].getClass())) {
                whereArgsList.add(mQuery.mWhereArgs[i].toString());
                paramReplacements.add("?");
                continue;
            }

            paramReplacements.add(String.valueOf(mQuery.mWhereArgs[i]));
        }

        final StringBuilder clauseBuilder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            clauseBuilder.append(split[i]);
            clauseBuilder.append(paramReplacements.get(i));
        }
        mQuery.mWhereClause = clauseBuilder.toString();
        mQuery.mWhereArgs = whereArgsList.toArray();
    }

    /**
     * Attaches an {@link SQLiteQuery} to the operating subclass.
     *
     * @param query the {@link SQLiteQuery} to attach.
     * @return itself
     */
    public T withQuery(final @NonNull SQLiteQuery query) {
        mQuery = query;
        if (mQuery.mWhereClause != null && mQuery.mWhereArgs != null) {
            fixNonStringParameters();
        }
        return (T) this;
    }
}