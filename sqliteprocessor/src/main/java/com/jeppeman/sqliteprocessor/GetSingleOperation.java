package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@SuppressWarnings("unchecked")
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
