//package com.jeppeman.liteomatic;
//
//import android.content.Context;
//import android.support.annotation.NonNull;
//import android.support.annotation.WorkerThread;
//
//import java.util.concurrent.Callable;
//
//import rx.Completable;
//import rx.android.schedulers.AndroidSchedulers;
//import rx.schedulers.Schedulers;
//
///**
// * This class inserts one or more rows into a table based on a mapping from {@link T}. The
// * insertion can be blocking or non-blocking returning {@link rx.Completable}s
// *
// * @param <T> the type of object to insert
// * @author jesper
// */
//public class InsertOperation<T> {
//
//    private final Context mContext;
//    private final SQLiteDAO<T>[] mObjectsToInsert;
//
//    InsertOperation(final @NonNull Context context,
//                    final @NonNull SQLiteDAO<T>[] objectsToInsert) {
//        mContext = context;
//        mObjectsToInsert = objectsToInsert;
//    }
//
//    /**
//     * Inserts one or more records into a table, blocking operation.
//     */
//    @WorkerThread
//    public void executeBlocking() {
//        for (final SQLiteDAO<T> objectToInsert : mObjectsToInsert) {
//            objectToInsert.insert(mContext);
//        }
//    }
//
//    /**
//     * Inserts one or more records into a table, non-blocking operation.
//     *
//     * @return a {@link rx.Completable}
//     */
//    public Completable execute() {
//        return Completable.fromCallable(new Callable() {
//            @Override
//            public Object call() throws Exception {
//                executeBlocking();
//                return null;
//            }
//        }).observeOn(AndroidSchedulers.mainThread())
//                .subscribeOn(Schedulers.io());
//    }
//}