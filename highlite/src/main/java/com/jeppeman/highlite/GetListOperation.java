package com.jeppeman.highlite;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * This class fetches one or more rows from a table and maps them to objects of type {@link T}. The
 * fetching can be blocking or non-blocking returning {@link rx.Observable}s
 *
 * @param <T> the type of object to map rows to
 * @author jesper
 */
public class GetListOperation<T> extends RawQueryableOperation<GetListOperation<T>> {

    private final Context mContext;
    private final SQLiteDAO<T> mGenerated;

    GetListOperation(final @NonNull Context context,
                     final @NonNull SQLiteDAO<T> generated) {
        mContext = context;
        mGenerated = generated;
    }

    /**
     * Fetches multiple rows from a database and maps them to objects of type {@link T}, blocking
     * operation.
     *
     * @return a list of objects of type {@link T} mapped from database records
     */
    @WorkerThread
    public List<T> executeBlocking() {
        if (mQuery != null) {
            final String[] whereArgsAsStringArray;
            if (mQuery.mWhereArgs != null) {
                whereArgsAsStringArray = new String[mQuery.mWhereArgs.length];
                for (int i = 0; i < mQuery.mWhereArgs.length; i++) {
                    whereArgsAsStringArray[i] = String.valueOf(mQuery.mWhereArgs[i]);
                }
            } else {
                whereArgsAsStringArray = null;
            }

            return mGenerated.getList(mContext, mQuery.mWhereClause,
                    whereArgsAsStringArray, mQuery.mGroupByClause, mQuery.mHavingClause,
                    mQuery.mOrderByClause, mQuery.mLimitClause, false);
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

            return mGenerated.getList(mContext, mRawQueryClause,
                    rawQueryArgsAsStringArray, false);
        } else {
            return mGenerated.getList(mContext, null, null, null, null, null, null, false);
        }
    }

    /**
     * Fetches multiple rows from a database and maps them to objects of type {@link T},
     * non-blocking operation.
     *
     * @return an {@link rx.Observable<T>} where an object of type {@link T} mapped from a database
     * record is passed as the parameter to {@link rx.Subscriber#onNext(Object)}}
     */
    public Observable<T> execute() {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final List<T> instanceList = executeBlocking();
                for (final T item : instanceList) {
                    subscriber.onNext(item);
                }
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}