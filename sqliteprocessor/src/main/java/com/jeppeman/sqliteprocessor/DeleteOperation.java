package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    @WorkerThread
    public void executeBlocking() {
        if (mObjectsToDelete != null) {
            for (final SQLiteDAO<T> objectToDelete : mObjectsToDelete) {
                objectToDelete.delete(mContext);
            }
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

            mGenerated.deleteByQuery(mContext, mQuery.mWhereClause,
                    whereArgsAsStringArray);
        } else if (mRawQueryClause != null && mGenerated != null) {
            final String[] rawQueryArgsAsStringArray;
            if (mRawQueryArgs != null) {
                rawQueryArgsAsStringArray = new String[mRawQueryArgs.length];
                for (int i = 0; i < mRawQueryArgs.length; i++) {
                    rawQueryArgsAsStringArray[i] = String.valueOf(mRawQueryArgs[i]);
                }
            } else {
                rawQueryArgsAsStringArray = null;
            }

            mGenerated.deleteByQuery(mContext, mRawQueryClause,
                    rawQueryArgsAsStringArray);
        }
    }

    public Completable execute() {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                executeBlocking();
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}