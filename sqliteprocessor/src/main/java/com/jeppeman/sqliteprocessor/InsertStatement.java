package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InsertStatement<T> extends Statement {

    private final Context mContext;
    private final SQLiteDAO<T>[] mObjectsToInsert;

    InsertStatement(final @NonNull Context context,
                    final @NonNull SQLiteDAO<T>[] objectsToInsert) {
        mContext = context;
        mObjectsToInsert = objectsToInsert;
    }

    @Override
    public PreparedInsertStatement prepareStatement() {
        return new PreparedInsertStatement();
    }

    public class PreparedInsertStatement implements PreparedStatement {

        PreparedInsertStatement() {

        }

        @WorkerThread
        public void executeBlocking() {
            for (final SQLiteDAO<T> objectToInsert : mObjectsToInsert) {
                objectToInsert.insert(mContext);
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
}
