package com.jeppeman.liteomatic;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * This class deletes one or more rows from a table. The deletion can be blocking or non-blocking
 * returning {@link rx.Single}s
 *
 * @param <T> the type of object to delete
 * @author jesper
 */
public class DeleteOperation<T> extends QueryableOperation<DeleteOperation<T>> {

    private final Context mContext;
    @Nullable
    private final SQLiteDAO<T> mGenerated;
    @Nullable
    private final SQLiteDAO<T>[] mObjectsToDelete;

    DeleteOperation(final @NonNull Context context,
                    final @Nullable SQLiteDAO<T> generated,
                    final @Nullable SQLiteDAO<T>[] objectsToDelete) {
        mContext = context;
        mGenerated = generated;
        mObjectsToDelete = objectsToDelete;
    }

    /**
     * Deletes one or more records from a table, blocking operation.
     *
     * @return the number of records removed
     */
    @WorkerThread
    public int executeBlocking() {
        if (mObjectsToDelete != null) {
            int nDeletedObjects = 0;
            for (final SQLiteDAO<T> objectToDelete : mObjectsToDelete) {
                nDeletedObjects += objectToDelete.delete(mContext);
            }
            return nDeletedObjects;
        } else if (mQuery != null && mGenerated != null) {
            final String[] whereArgsAsStringArray;
            if (mQuery.mWhereArgs != null) {
                whereArgsAsStringArray = new String[mQuery.mWhereArgs.length];
                for (int i = 0; i < mQuery.mWhereArgs.length; i++) {
                    whereArgsAsStringArray[i] = String.valueOf(mQuery.mWhereArgs[i]);
                }
            } else {
                whereArgsAsStringArray = null;
            }

            return mGenerated.deleteByQuery(mContext, mQuery.mWhereClause,
                    whereArgsAsStringArray);
        }

        return 0;
    }

    /**
     * Deletes one or more records from a table, non-blocking operation.
     *
     * @return a {@link rx.Single<Integer>} where the number of records deleted is passed
     * as the parameter to {@link rx.SingleSubscriber#onSuccess(Object)}
     */
    public Single<Integer> execute() {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}