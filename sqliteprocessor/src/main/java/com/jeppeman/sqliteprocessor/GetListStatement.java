package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GetListStatement<T> extends QueryableStatement {

    private final Context mContext;
    private final SQLiteDAO<T> mGenerated;

    GetListStatement(final @NonNull Context context,
                            final @NonNull SQLiteDAO<T> generated) {
        mContext = context;
        mGenerated = generated;
    }

    @Override
    public PreparedGetListStatement prepareStatement() {
        return new PreparedGetListStatement();
    }

    public class PreparedGetListStatement implements PreparedStatement {

        PreparedGetListStatement() {

        }

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
                        mQuery.mOrderByClause, mQuery.mLimitClause);
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
                        rawQueryArgsAsStringArray);
            } else {
                return mGenerated.getList(mContext, null, null, null, null, null, null);
            }
        }

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
}
