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

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Delegates calls to the generated DAO and returns {@link Observable}s
 *
 * @author jesper
 */
public abstract class SQLiteObject {

    private static final Map<Class<?>, Constructor> CTOR_CACHE = new LinkedHashMap<>();

    protected transient ObjectState mObjectState;

    protected SQLiteObject() {
        mObjectState = ObjectState.NEW;
    }

    @SuppressWarnings("unchecked")
    @WorkerThread
    static <T extends SQLiteObject> SQLiteDAO<T> getGeneratedObject(
            final @NonNull Class<T> cls,
            final @Nullable T generator) {
        Constructor<SQLiteDAO<T>> generatedCtor = null;
        try {
            generatedCtor = CTOR_CACHE.get(cls);
            if (generatedCtor != null) return generatedCtor.newInstance();

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

    public static <T extends SQLiteObject> Observable<T> getSingle(final @NonNull Context context,
                                                                   final @NonNull Object id,
                                                                   final @NonNull Class<T> cls) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final T instance = getSingleBlocking(context, id, cls);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T extends SQLiteObject> T getSingleBlocking(final @NonNull Context context,
                                                               final @NonNull Object id,
                                                               final @NonNull Class<T> cls) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final T instance = generated.getSingle(context, id);
        instance.mObjectState = ObjectState.EXISTING;

        return instance;
    }

    public static <T extends SQLiteObject> Observable<List<T>> getCustom(
            final @NonNull Context context,
            final @Nullable String customWhere,
            final @NonNull Class<T> cls) {

        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                final List<T> instanceList = getCustomBlocking(context, customWhere, cls);
                subscriber.onNext(instanceList);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T extends SQLiteObject> List<T> getCustomBlocking(
            final @NonNull Context context,
            final @Nullable String customWhere,
            final @NonNull Class<T> cls) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final List<T> instanceList = generated.getCustom(context, customWhere);
        for (final T instance : instanceList) {
            instance.mObjectState = ObjectState.EXISTING;
        }

        return instanceList;
    }

    public static <T extends SQLiteObject> Observable<List<T>> getAll(
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

    public static <T extends SQLiteObject> List<T> getAllBlocking(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {
        final SQLiteDAO<T> generated = getGeneratedObject(cls, null);
        final List<T> instanceList = generated.getCustom(context, null);
        for (final T instance : instanceList) {
            instance.mObjectState = ObjectState.EXISTING;
        }

        return instanceList;
    }

    @SuppressWarnings("unchecked")
    <T extends SQLiteObject> void dbInsertBlocking(final @NonNull Context context) {
        final SQLiteDAO<T> generated = getGeneratedObject(
                (Class<T>) getClass(), (T) this);
        generated.insert(context);
        mObjectState = ObjectState.EXISTING;
    }

    @SuppressWarnings("unchecked")
    <T extends SQLiteObject> void dbUpdateBlocking(final @NonNull Context context) {
        final SQLiteDAO<T> generated = getGeneratedObject(
                (Class<T>) SQLiteObject.this.getClass(), (T) SQLiteObject.this);
        generated.update(context);
    }

    @SuppressWarnings("unchecked")
    private <T extends SQLiteObject> Observable<T> dbInsert(final @NonNull Context context) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                dbInsertBlocking(context);
                subscriber.onNext((T) SQLiteObject.this);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    @SuppressWarnings("unchecked")
    private <T extends SQLiteObject> Observable<T> dbUpdate(final @NonNull Context context) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                dbUpdateBlocking(context);
                subscriber.onNext((T) SQLiteObject.this);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public <T extends SQLiteObject> Observable<T> save(final @NonNull Context context) {
        switch (mObjectState) {
            case NEW:
                return dbInsert(context);
            case EXISTING:
                return dbUpdate(context);
        }

        return null;
    }

    public void saveBlocking(final @NonNull Context context) {
        switch (mObjectState) {
            case NEW:
                dbInsertBlocking(context);
            case EXISTING:
                dbUpdateBlocking(context);
        }
    }

    @SuppressWarnings("unchecked")
    public Observable delete(final @NonNull Context context) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                deleteBlocking(context);
                subscriber.onNext(null);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    @SuppressWarnings("unchecked")
    public <T extends SQLiteObject> void deleteBlocking(final @NonNull Context context) {
        final SQLiteDAO<T> generated = getGeneratedObject(
                (Class<T>) SQLiteObject.this.getClass(), (T) SQLiteObject.this);
        generated.delete(context);
    }

    protected enum ObjectState {
        NEW,
        EXISTING
    }
}