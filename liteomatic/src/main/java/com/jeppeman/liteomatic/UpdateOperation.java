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
 * This class updates one or more rows in a table based on a mapping from the type {@link T}. The
 * updating can be blocking or non-blocking returning {@link rx.Single}s
 *
 * @param <T> the type of object to insert
 * @author jesper
 */
public class UpdateOperation<T> extends QueryableOperation<UpdateOperation<T>> {

    private final Context mContext;
    @Nullable
    private final SQLiteDAO<T> mGenerated;
    @Nullable
    private final SQLiteDAO<T>[] mObjectsToUpdate;

    UpdateOperation(final @NonNull Context context,
                    final @Nullable SQLiteDAO<T> generated,
                    final @Nullable SQLiteDAO<T>[] objectsToUpdate) {
        mContext = context;
        mGenerated = generated;
        mObjectsToUpdate = objectsToUpdate;
    }

    /**
     * Updates one or more records in a table, blocking operation.
     *
     * @return the number of records updated
     */
    @WorkerThread
    public int executeBlocking() {
        if (mObjectsToUpdate != null) {
            int nUpdatedObjects = 0;
            for (final SQLiteDAO<T> objectToUpdate : mObjectsToUpdate) {
                nUpdatedObjects += objectToUpdate.update(mContext);
            }
            return nUpdatedObjects;
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

            return mGenerated.updateByQuery(mContext, mQuery.mWhereClause,
                    whereArgsAsStringArray);
        }

        return 0;
    }

    /**
     * Updates one or more records in a table, non-blocking operation.
     *
     * @return a {@link rx.Single<Integer>} where the number of records updated is passed
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
