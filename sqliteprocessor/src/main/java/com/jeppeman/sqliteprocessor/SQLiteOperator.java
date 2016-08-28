package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Delegates calls to the generated DAO and returns {@link Observable}s
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

    public static <T> Observable<T> getSingle(final @NonNull Context context,
                                  final @NonNull Class<T> cls,
                                  final @NonNull SQLiteQuery query) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final T instance = getSingleBlocking(context, cls, query);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> T getSingleBlocking(final @NonNull Context context,
                                          final @NonNull Class<T> cls,
                                          final @NonNull Object id) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        return generated.getSingle(context, id);
    }

    public static <T> Observable<T> getSingle(final @NonNull Context context,
                                              final @NonNull Class<T> cls,
                                              final @NonNull Object id) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final T instance = getSingleBlocking(context, cls, id);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> List<T> getCustomBlocking(
            final @NonNull Context context,
            final @NonNull Class<T> cls,
            final @Nullable SQLiteQuery query) {
        final List<T> instanceList;
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        if (query == null) {
            instanceList = generated.getCustom(context, null, null, null, null, null);
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
            instanceList = generated.getCustom(context, query.mWhereClause, whereArgsAsStringArray,
                    query.mGroupByClause, query.mHavingClause, query.mOrderByClause);
        }

        return instanceList;
    }

    public static <T> Observable<List<T>> getCustom(
            final @NonNull Context context,
            final @NonNull Class<T> cls,
            final @Nullable SQLiteQuery query) {

        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                final List<T> instanceList = getCustomBlocking(context, cls, query);
                subscriber.onNext(instanceList);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> List<T> getAllBlocking(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        return generated.getCustom(context, null, null, null, null, null);
    }

    public static <T> Observable<List<T>> getAll(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {

        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                final List<T> instanceList = getAllBlocking(context, cls);
                subscriber.onNext(instanceList);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> void insertBlocking(final @NonNull Context context,
                                          final @NonNull T objectToInsert) {
        final SQLiteDAO<T> generated = getGeneratedObject(
                (Class<T>) objectToInsert.getClass(), objectToInsert);
        generated.insert(context);
    }

    public static <T> Observable<T> insert(final @NonNull Context context,
                                           final @NonNull T objectToInsert) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                insertBlocking(context, objectToInsert);
                subscriber.onNext(objectToInsert);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> void updateBlocking(final @NonNull Context context,
                                          final @NonNull T objectToUpdate) {
        final SQLiteDAO<T> generated = getGeneratedObject((Class<T>) objectToUpdate.getClass(),
                objectToUpdate);
        generated.update(context);
    }

    private <T> Observable<T> update(final @NonNull Context context,
                                     final @NonNull T objectToUpdate) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                updateBlocking(context, objectToUpdate);
                subscriber.onNext(objectToUpdate);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T> void deleteBlocking(
            final @NonNull Context context,
            final @NonNull T objectToDelete) {
        final SQLiteDAO<T> generated = getGeneratedObject(
                (Class<T>) objectToDelete.getClass(), objectToDelete);
        generated.delete(context);
    }

    public static <T> Observable delete(final @NonNull Context context,
                                        final @NonNull T objectToDelete) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                deleteBlocking(context, objectToDelete);
                subscriber.onNext(null);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }
}