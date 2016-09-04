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
public final class SQLiteOperator {

    private static final Map<Class<?>, Constructor> CTOR_CACHE = new LinkedHashMap<>();

    private SQLiteOperator() {

    }

    static <T> SQLiteDAO<T> getGeneratedObject(
            final @NonNull Class<T> cls,
            final @Nullable T generator) {
        Constructor<SQLiteDAO<T>> generatedCtor = null;
        try {
            generatedCtor = CTOR_CACHE.get(cls);
            if (generatedCtor != null) return generatedCtor.newInstance(generator);

            final Class<SQLiteDAO<T>> clazz = (Class<SQLiteDAO<T>>)
                    Class.forName(cls.getCanonicalName() + "_DAO");

            generatedCtor = clazz.getConstructor(cls);
            CTOR_CACHE.put(cls, generatedCtor);

            return generatedCtor.newInstance(generator);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + generatedCtor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke " + generatedCtor, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Generated class not found", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find ctor for " + cls.getName() + "_DAO", e);
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
    public static <T> T getSingleBlocking(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull String rawQueryClause,
                                          final @Nullable Object... rawQueryArgs) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final String[] rawQueryArgsAsStringArray;
        if (rawQueryArgs != null) {
            rawQueryArgsAsStringArray = new String[rawQueryArgs.length];
            for (int i = 0; i < rawQueryArgs.length; i++) {
                rawQueryArgsAsStringArray[i] = String.valueOf(rawQueryArgs[i]);
            }
        } else {
            rawQueryArgsAsStringArray = null;
        }

        return generated.getSingle(context, rawQueryClause, rawQueryArgsAsStringArray);
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
    public static <T> Single<T> getSingle(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull String rawQueryClause,
                                          final @Nullable Object... rawQueryArgs) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return getSingleBlocking(context, cls, rawQueryClause, rawQueryArgs);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Fetches a single object of type {@link T}, blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param query   query specifying which row to fetch
     * @param <T>     type to fetch
     * @return an instance of type {@link T} with database fields mapped to class fields
     * annotated with {@link SQLiteField} based on the query supplied if found, else null
     */
    @WorkerThread
    @Nullable
    public static <T> T getSingleBlocking(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull SQLiteQuery query) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final String[] whereArgsAsStringArray;
        if (query.mWhereArgs != null) {
            whereArgsAsStringArray = new String[query.mWhereArgs.length];
            for (int i = 0; i < query.mWhereArgs.length; i++) {
                whereArgsAsStringArray[i] = String.valueOf(query.mWhereArgs[i]);
            }
        } else {
            whereArgsAsStringArray = null;
        }
        return generated.getSingle(context, query.mWhereClause, whereArgsAsStringArray,
                query.mGroupByClause, query.mHavingClause, query.mOrderByClause);
    }

    /**
     * Fetches a single object of type {@link T}, non-blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param query   query specifying which row to fetch
     * @param <T>     type to fetch
     * @return an {@link Observable} where an instance of type {@link T} is passed as the
     * item in {@link Subscriber#onNext(Object)}
     */
    public static <T> Single<T> getSingle(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull SQLiteQuery query) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return getSingleBlocking(context, cls, query);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Fetches a single object with id of type {@link T}, blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param id      id of the row to fetch
     * @param <T>     type to fetch
     * @return an instance of type {@link T} with database fields mapped to class fields
     * annotated with {@link SQLiteField}
     */
    @WorkerThread
    @Nullable
    public static <T> T getSingleBlocking(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull Object id) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        return generated.getSingle(context, id);
    }

    /**
     * Fetches a single object with id of type {@link T}, non-blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param id      id of the row to fetch
     * @param <T>     type to fetch
     * @return an {@link Observable} where an instance of type {@link T} is passed as the
     * item in {@link Subscriber#onNext(Object)}
     */
    public static <T> Single<T> getSingle(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull Object id) {
        return Single.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return getSingleBlocking(context, cls, id);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
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
    public static <T> List<T> getListBlocking(final @NonNull Context context,
                                              final @NonNull Class<T> cls,
                                              final @NonNull String rawQueryClause,
                                              final @Nullable Object... rawQueryArgs) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final String[] rawQueryArgsAsStringArray;
        if (rawQueryArgs != null) {
            rawQueryArgsAsStringArray = new String[rawQueryArgs.length];
            for (int i = 0; i < rawQueryArgs.length; i++) {
                rawQueryArgsAsStringArray[i] = String.valueOf(rawQueryArgs[i]);
            }
        } else {
            rawQueryArgsAsStringArray = null;
        }

        return generated.getList(context, rawQueryClause, rawQueryArgsAsStringArray);
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
    public static <T> Observable<T> getList(final @NonNull Context context,
                                            final @NonNull Class<T> cls,
                                            final @NonNull String rawQueryClause,
                                            final @Nullable Object... rawQueryArgs) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final List<T> instanceList = getListBlocking(context, cls, rawQueryClause,
                        rawQueryArgs);
                for (final T item : instanceList) {
                    subscriber.onNext(item);
                }
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Fetches a {@link List} of {@link T} filtered by an {@link SQLiteQuery, blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param query   query specifying which rows to fetch
     * @param <T>     type to fetch
     * @return a {@link List} of {@link T} filtered by {@link SQLiteQuery}
     */
    @WorkerThread
    public static <T> List<T> getListBlocking(
            final @NonNull Context context,
            final @NonNull Class<T> cls,
            final @Nullable SQLiteQuery query) {
        final List<T> instanceList;
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        if (query == null) {
            instanceList = generated.getList(context, null, null, null, null, null, null);
        } else {
            final String[] whereArgsAsStringArray;
            if (query.mWhereArgs != null) {
                whereArgsAsStringArray = new String[query.mWhereArgs.length];
                for (int i = 0; i < query.mWhereArgs.length; i++) {
                    whereArgsAsStringArray[i] = String.valueOf(query.mWhereArgs[i]);
                }
            } else {
                whereArgsAsStringArray = null;
            }
            instanceList = generated.getList(context, query.mWhereClause, whereArgsAsStringArray,
                    query.mGroupByClause, query.mHavingClause, query.mOrderByClause,
                    query.mLimitClause);
        }

        return instanceList;
    }

    /**
     * Fetches a {@link List} of {@link T} filtered by an {@link SQLiteQuery, non-blocking
     * operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param query   query specifying which rows to fetch
     * @param <T>     type to fetch
     * @return an {@link Observable} where a {@link List} of type {@link T} is passed as the
     * item in {@link Subscriber#onNext(Object)}
     */
    public static <T> Observable<T> getList(
            final @NonNull Context context,
            final @NonNull Class<T> cls,
            final @Nullable SQLiteQuery query) {

        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final @NonNull Subscriber<? super T> subscriber) {
                final List<T> instanceList = getListBlocking(context, cls, query);
                for (final T item : instanceList) {
                    subscriber.onNext(item);
                }
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Fetches a full {@link List} of {@link T}, blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param <T>     type to fetch
     * @return a {@link List} of {@link T} from all table records
     */
    @WorkerThread
    public static <T> List<T> getFullListBlocking(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        return generated.getList(context, null, null, null, null, null, null);
    }

    /**
     * Fetches a full {@link List} of {@link T}, non-blocking operation.
     *
     * @param context the context from which the call is being made
     * @param cls     class object of the type to fetch
     * @param <T>     type to fetch
     * @return an {@link Observable} where a {@link List} of type {@link T} from all table records
     * is passed as the item in {@link Subscriber#onNext(Object)}
     */
    public static <T> Observable<T> getFullList(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {

        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final @NonNull Subscriber<? super T> subscriber) {
                final List<T> instanceList = getFullListBlocking(context, cls);
                for (final T item : instanceList) {
                    subscriber.onNext(item);
                }
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Inserts an object of type {@link T} into a database, blocking operation.
     *
     * @param context         the context from which the call is being made
     * @param objectsToInsert the object to insert
     * @param <T>             type of the object to insert
     */
    @WorkerThread
    public static <T> void insertBlocking(final @NonNull Context context,
                                          final @NonNull T... objectsToInsert) {
        for (final T objectToInsert : objectsToInsert) {
            final SQLiteDAO<T> generated = getGeneratedObject(
                    (Class<T>) objectToInsert.getClass(), objectToInsert);
            generated.insert(context);
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
    public static <T> Completable insert(final @NonNull Context context,
                                         final @NonNull T... objectsToInsert) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                insertBlocking(context, objectsToInsert);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Updates a database record based on an object of type {@link T}, blocking operation.
     *
     * @param context        the context from which the call is being made
     * @param objectToUpdate the object to update
     * @param <T>            type of the object to update
     */
    @WorkerThread
    public static <T> void updateBlocking(final @NonNull Context context,
                                          final @NonNull T objectToUpdate) {
        final SQLiteDAO<T> generated = getGeneratedObject((Class<T>) objectToUpdate.getClass(),
                objectToUpdate);
        generated.update(context);
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
    private <T> Completable update(final @NonNull Context context,
                                   final @NonNull T objectToUpdate) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                updateBlocking(context, objectToUpdate);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * Deletes a database record based on an object of type {@link T}
     *
     * @param context         the context from which the call is being made
     * @param objectsToDelete the objects to delete
     * @param <T>             type of the object to delete
     */
    @WorkerThread
    public static <T> void deleteBlocking(final @NonNull Context context,
                                          final @NonNull T... objectsToDelete) {
        for (final T objectToDelete : objectsToDelete) {
            final SQLiteDAO<T> generated = getGeneratedObject(
                    (Class<T>) objectToDelete.getClass(), objectToDelete);
            generated.delete(context);
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
    public static <T> Completable delete(final @NonNull Context context,
                                         final @NonNull T... objectsToDelete) {
        return Completable.fromCallable(new Callable() {
            @Override
            public Object call() throws Exception {
                deleteBlocking(context, objectsToDelete);
                return null;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }
}