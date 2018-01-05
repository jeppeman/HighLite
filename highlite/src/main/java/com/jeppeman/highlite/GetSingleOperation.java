package com.jeppeman.highlite;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * This class fetches single rows from a table and maps them to objects of type {@link T}. The
 * fetching can be blocking or non-blocking returning {@link Single<T>}s
 *
 * @param <T> the type of object to map rows to
 * @author jesper
 */
public class GetSingleOperation<T> extends RawQueryableOperation<GetSingleOperation<T>> {

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
                    mQuery.mOrderByClause, false);
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
                    rawQueryArgsAsStringArray, false);
        }

        throw new RuntimeException("No id or query provided to getSingle");
    }

    /**
     * Fetches a single row from a database and maps it to and object of type {@link T},
     * non-blocking operation.
     *
     * @return a {@link Maybe<T>} where an object of type {@link T} mapped from a database
     * record is passed as the parameter to
     * {@link io.reactivex.observers.DisposableMaybeObserver#onSuccess(Object)}
     */
    public Maybe<T> execute() {
        return Maybe.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return executeBlocking();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}