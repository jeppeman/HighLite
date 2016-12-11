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
 * This class fetches single rows from a table and maps them to objects of type {@link T}. The
 * fetching can be blocking or non-blocking returning {@link rx.Single}s
 *
 * @param <T> the type of object to map rows to
 * @author jesper
 */
public class GetSingleOperation<T> extends QueryableOperation<GetSingleOperation<T>> {

    private final Context mContext;
    private final SQLiteDAO<T> mGenerated;
    @Nullable
    private final Object mId;

    GetSingleOperation(final @NonNull Context context,
                       final @NonNull SQLiteDAO<T> generated,
                       final @Nullable Object id) {
        mContext = context;
        mGenerated = generated;
        mId = id;
    }

    /**
     * Fetches a single row from a database and maps it to an object of type {@link T}, blocking
     * operation.
     *
     * @return an object of type {@link T} mapped from a database record
     */
    @WorkerThread
    @Nullable
    public T executeBlocking() {
        if (mId != null) {
            return mGenerated.getSingle(mContext, mId);
        } else if (mQuery != null) {
            final String[] whereArgsAsStringArray;
            if (mQuery.mWhereArgs != null) {
                whereArgsAsStringArray = new String[mQuery.mWhereArgs.length];
                for (int i = 0; i < mQuery.mWhereArgs.length; i++) {
                    whereArgsAsStringArray[i] = String.valueOf(mQuery.mWhereArgs[i]);
                }
            } else {
                whereArgsAsStringArray = null;
            }

            return mGenerated.getSingle(mContext, mQuery.mWhereClause,
                    whereArgsAsStringArray, mQuery.mGroupByClause, mQuery.mHavingClause,
                    mQuery.mOrderByClause);
        } else if (mRawQueryClause != null) {
            final String[] rawQueryArgsAsStringArray;
            if (mRawQueryArgs != null) {
                rawQueryArgsAsStringArray = new String[mRawQueryArgs.length];
                for (int i = 0; i < mRawQueryArgs.length; i++) {
                    rawQueryArgsAsStringArray[i] = String.valueOf(mRawQueryArgs[i]);
                }
            } else {
                rawQueryArgsAsStringArray = null;
            }

            return mGenerated.getSingle(mContext, mRawQueryClause,
                    rawQueryArgsAsStringArray);
        }

        throw new RuntimeException("No id or query provided to getSingle");
    }

    /**
     * Fetches a single row from a database and maps it to and object of type {@link T},
     * non-blocking operation.
     *
     * @return a {@link rx.Single<T>} where an object of type {@link T} mapped from a database
     * record is passed as the parameter to {@link rx.SingleSubscriber#onSuccess(Object)}
     */
    public Single<T> execute() {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return executeBlocking();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}