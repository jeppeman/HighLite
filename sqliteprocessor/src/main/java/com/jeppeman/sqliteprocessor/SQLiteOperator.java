package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Performs database operations by delegating calls to a generated DAO. Operations can
 * be blocking or run with {@link Observable}s
 *
 * @author jesper
 */
@SuppressWarnings({"unchecked", "unused"})
public final class SQLiteOperator<T> {

    private static final Map<Class<?>, Constructor> CTOR_CACHE = new LinkedHashMap<>();

    private final Class<T> mClass;
    private final Context mContext;
    private String mRawQueryClause;
    private Object[] mRawQueryArgs;
    private SQLiteQuery mQuery;

    private SQLiteOperator(final @NonNull Context context, final @NonNull Class<T> cls) {
        mClass = cls;
        mContext = context;
    }

    public static SQLiteOperator from(final @NonNull Context context,
                                      final @NonNull Class<?> cls) {
        return new SQLiteOperator(context, cls);
    }

    SQLiteDAO<T> getGeneratedObject(final @Nullable T generator) {
        Constructor<SQLiteDAO<T>> generatedCtor = null;
        try {
            generatedCtor = CTOR_CACHE.get(mClass);
            if (generatedCtor != null) return generatedCtor.newInstance(generator);

            final Class<SQLiteDAO<T>> clazz = (Class<SQLiteDAO<T>>)
                    Class.forName(mClass.getCanonicalName() + "_DAO");

            generatedCtor = clazz.getConstructor(mClass);
            CTOR_CACHE.put(mClass, generatedCtor);

            return generatedCtor.newInstance(generator);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + generatedCtor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke " + generatedCtor, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Generated class not found", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find ctor for " + mClass.getName() + "_DAO", e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    public SQLiteOperator withRawQuery(final @NonNull String rawQueryClause,
                                       final @Nullable Object... rawQueryArgs) {
        mQuery = null;
        mRawQueryClause = rawQueryClause;
        mRawQueryArgs = rawQueryArgs;
        return this;
    }

    public SQLiteOperator withQuery(final @NonNull SQLiteQuery query) {
        mRawQueryClause = null;
        mRawQueryArgs = null;
        mQuery = query;
        return this;
    }

    /**
     * Fetches an object of type {@link T} based on a raw query, blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param cls            class object of the type to fetch
     * @param rawQueryClause an SQLite command with where ? is a parameter
     * @param rawQueryArgs   parameter values for the query clause
     * @param <T>            type to fetch
     * @return an instance of {@link T} based on the raw query supplied if found, else null
     */
    @WorkerThread
    @Nullable
    public T getSingleBlocking(final @Nullable Object id) {
        final SQLiteDAO<T> generated = getGeneratedObject(null);
        if (mRawQueryClause != null) {
            final String[] rawQueryArgsAsStringArray;
            if (mRawQueryArgs != null) {
                rawQueryArgsAsStringArray = new String[mRawQueryArgs.length];
                for (int i = 0; i < mRawQueryArgs.length; i++) {
                    rawQueryArgsAsStringArray[i] = String.valueOf(mRawQueryArgs[i]);
                }
            } else {
                rawQueryArgsAsStringArray = null;
            }

            return generated.getSingle(mContext, mRawQueryClause, rawQueryArgsAsStringArray);
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

            return generated.getSingle(mContext, mQuery.mWhereClause, whereArgsAsStringArray,
                    mQuery.mGroupByClause, mQuery.mHavingClause, mQuery.mOrderByClause);
        } else if (id != null) {
            return generated.getSingle(mContext, id);
        }

        throw new RuntimeException("No ");
    }

    @WorkerThread
    @Nullable
    public T getSingleBlocking() {
        return getSingleBlocking(null);
    }

    /**
     * Fetches an object of type {@link T} based on a raw query, non-blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param cls            class object of the type to fetch
     * @param rawQueryClause an SQLite command with where ? is a parameter
     * @param rawQueryArgs   parameter values for the query clause
     * @param <T>            type to fetch
     * @return an {@link Observable} where an instance of type {@link T} based on the raw query
     * is passed as the item in {@link Subscriber#onNext(Object)}
     */
    public Single<T> getSingle(final @Nullable Object id) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return getSingleBlocking(id);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    public Single<T> getSingle() {
        return getSingle(null);
    }

    /**
     * Fetches a {@link List} of type {@link T} based on a raw query, blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param cls            class object of the type to fetch
     * @param rawQueryClause an SQLite command with where ? is a parameter
     * @param rawQueryArgs   parameter values for the query clause
     * @param <T>            type to fetch
     * @return a {@link List} of type {@link T} based on the raw query supplied
     */
    @WorkerThread
    public List<T> getListBlocking() {
        final SQLiteDAO<T> generated = getGeneratedObject(null);
        if (mRawQueryClause != null) {
            final String[] rawQueryArgsAsStringArray;
            if (mRawQueryArgs != null) {
                rawQueryArgsAsStringArray = new String[mRawQueryArgs.length];
                for (int i = 0; i < mRawQueryArgs.length; i++) {
                    rawQueryArgsAsStringArray[i] = String.valueOf(mRawQueryArgs[i]);
                }
            } else {
                rawQueryArgsAsStringArray = null;
            }

            return generated.getList(mContext, mRawQueryClause, rawQueryArgsAsStringArray);
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
            return generated.getList(mContext, mQuery.mWhereClause, whereArgsAsStringArray,
                    mQuery.mGroupByClause, mQuery.mHavingClause, mQuery.mOrderByClause,
                    mQuery.mLimitClause);
        } else {
            return generated.getList(mContext, null, null, null, null, null, null);
        }
    }

    /**
     * Fetches a {@link List} of type {@link T} based on a raw query, non-blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param cls            class object of the type to fetch
     * @param rawQueryClause an SQLite command with where ? is a parameter
     * @param rawQueryArgs   parameter values for the query clause
     * @param <T>            type to fetch
     * @return an {@link Observable} where a {@link List} of type {@link T} based on the raw query
     * is passed as the item in {@link Subscriber#onNext(Object)}
     */
    public Observable<T> getList() {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final List<T> instanceList = getListBlocking();
                for (final T item : instanceList) {
                    subscriber.onNext(item);
                }
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    /**
     * Inserts an object of type {@link T} into a database, blocking operation.
     *
     * @param context         the context from which the call is being made
     * @param objectsToInsert the object to insert
     * @param <T>             type of the object to insert
     */
    @WorkerThread
    public void insertBlocking(final @NonNull T... objectsToInsert) {
        for (final T objectToInsert : objectsToInsert) {
            final SQLiteDAO<T> generated = getGeneratedObject(objectToInsert);
            generated.insert(mContext);
        }
    }

    /**
     * Inserts an object of type {@link T} into a database, non-blocking operation.
     *
     * @param context         the context from which the call is being made
     * @param objectsToInsert the object to insert
     * @param <T>             type of the object to insert
     * @return an {@link Observable} where the objectToInsert parameter is passed as the item
     * in {@link Subscriber#onNext(Object)}
     */
    public Completable insert(final @NonNull T... objectsToInsert) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                insertBlocking(objectsToInsert);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    /**
     * Updates a database record based on an object of type {@link T}, blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param objectToUpdate the object to update
     * @param <T>            type of the object to update
     */
    @WorkerThread
    public void updateBlocking(final @NonNull T objectToUpdate) {
        final SQLiteDAO<T> generated = getGeneratedObject(objectToUpdate);
        generated.update(mContext);
    }

    /**
     * Updates a database record based on an object of type {@link T}, non-blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param objectToUpdate the object to update
     * @param <T>            type of the object to update
     * @return an {@link Observable} where the objectToUpdate parameter is passed as the item
     * in {@link Subscriber#onNext(Object)}
     */
    private Completable update(final @NonNull T objectToUpdate) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                updateBlocking(objectToUpdate);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    /**
     * Deletes a database record based on an object of type {@link T}
     *
     * @param context         the context from which the call is being made
     * @param objectsToDelete the objects to delete
     * @param <T>             type of the object to delete
     */
    @WorkerThread
    public void deleteBlocking(final @NonNull T... objectsToDelete) {
        for (final T objectToDelete : objectsToDelete) {
            final SQLiteDAO<T> generated = getGeneratedObject(objectToDelete);
            generated.delete(mContext);
        }
    }

    /**
     * Deletes a database record based on an object of type {@link T}
     *
     * @param context         the context from which the call is being made
     * @param objectsToDelete the object to delete
     * @param <T>             type of the object to delete
     * @return an {@link Observable} where null is passed as the item in
     * {@link Subscriber#onNext(Object)}
     */
    public Completable delete(final @NonNull T... objectsToDelete) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                deleteBlocking(objectsToDelete);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }
}