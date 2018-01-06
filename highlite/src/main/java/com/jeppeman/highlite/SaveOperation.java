package com.jeppeman.highlite;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;


/**
 * This class saves (inserts or updates) one or more rows in a table based on a mapping from the
 * type {@link T}. The saving can be blocking or non-blocking returning {@link Single}s
 *
 * @param <T> the type of object to save
 * @author jesper
 */
public class SaveOperation<T> extends QueryableOperation<SaveOperation<T>>
        implements Operation<Integer, Integer> {

    private final Context mContext;
    @Nullable
    private final SQLiteDAO<T> mGenerated;
    @Nullable
    private final SQLiteDAO<T>[] mObjectsToSave;

    SaveOperation(final @NonNull Context context,
                  final @Nullable SQLiteDAO<T> generated,
                  final @Nullable SQLiteDAO<T>[] objectsToSave) {
        mContext = context;
        mGenerated = generated;
        mObjectsToSave = objectsToSave;
    }

    /**
     * Saves one or more records in a table, blocking operation.
     *
     * @return the number of records saved
     */
    @WorkerThread
    public int executeBlocking() {
        if (mObjectsToSave != null && mObjectsToSave.length > 0) {
            int nSavedObjects = 0;
            for (final SQLiteDAO<T> objectToUpdate : mObjectsToSave) {
                nSavedObjects += objectToUpdate.save(mContext);
            }
            return nSavedObjects;
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

            return mGenerated.saveByQuery(mContext, mQuery.mColsToBeSaved, mQuery.mWhereClause,
                    whereArgsAsStringArray);
        }

        return 0;
    }

    /**
     * Saves one or more records in a table, non-blocking operation.
     *
     * @param strategy the backpressure strategy used for the {@link Flowable}.
     *                 (see {@link BackpressureStrategy})
     * @return a {@link Flowable<Integer>} where the number of records saved is passed
     * as the parameter to {@link io.reactivex.observers.DisposableObserver#onNext(Object)}
     */
    @Override
    public Flowable<Integer> asFlowable(BackpressureStrategy strategy) {
        return Flowable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Saves one or more records in a table, non-blocking operation.
     *
     * @return a {@link Observable<Integer>} where the number of records saved is passed
     * as the parameter to {@link io.reactivex.observers.DisposableObserver#onNext(Object)}
     */
    @Override
    public Observable<Integer> asObservable() {
        return Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Saves one or more records in a table, non-blocking operation.
     *
     * @return a {@link Single<Integer>} where the number of records saved is passed
     * as the parameter to {@link io.reactivex.observers.DisposableSingleObserver#onSuccess(Object)}
     */
    @Override
    public Single<Integer> asSingle() {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Saves one or more records in a table, non-blocking operation.
     *
     * @return a {@link Maybe<Integer>} where the number of records saved is passed
     * as the parameter to {@link io.reactivex.observers.DisposableMaybeObserver#onSuccess(Object)}
     */
    @Override
    public Maybe<Integer> asMaybe() {
        return Maybe.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Saves one or more records in a table, non-blocking operation.
     *
     * @return a {@link Completable}
     */
    @Override
    public Completable asCompletable() {
        return Completable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeBlocking();
            }
        }).subscribeOn(Schedulers.io());
    }
}
