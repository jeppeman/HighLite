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
 * Created by jesper on 2016-08-25.
 */
public class SQLiteObject {

    private static final Map<Class<?>, Constructor> CTOR_CACHE = new LinkedHashMap<>();

    protected transient ObjectState mObjectState;

    protected SQLiteObject() {
        mObjectState = ObjectState.NEW;
    }

    @SuppressWarnings("unchecked")
    @WorkerThread
    private static <T extends SQLiteObject> SQLiteDAO<T> getWrappingObject(
            final @NonNull Class<T> cls) {
        Constructor<SQLiteDAO<T>> wrappingCtor = null;
        try {
            wrappingCtor = CTOR_CACHE.get(cls);
            if (wrappingCtor != null) return wrappingCtor.newInstance();

            final Class<SQLiteDAO<T>> clazz = (Class<SQLiteDAO<T>>)
                    Class.forName(cls.getName() + "_DAO");

            wrappingCtor = clazz.getConstructor();
            CTOR_CACHE.put(cls, wrappingCtor);

            return wrappingCtor.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + wrappingCtor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke " + wrappingCtor, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Wrapping class not found", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find ctor for " + cls.getName(), e);
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
                final SQLiteDAO<T> wrapper = getWrappingObject(cls);
                T instance = wrapper.getSingle(context, id);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T extends SQLiteObject> Observable<List<T>> getCustom(
            final @NonNull Context context,
            final @Nullable String customWhere,
            final @NonNull Class<T> cls) {

        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                final SQLiteDAO<T> wrapper = getWrappingObject(cls);
                List<T> instance = wrapper.getCustom(context, customWhere);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    public static <T extends SQLiteObject> Observable<List<T>> getAll(
            final @NonNull Context context,
            final @NonNull Class<T> cls) {

        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                final SQLiteDAO<T> wrapper = getWrappingObject(cls);
                List<T> instance = wrapper.getCustom(context, null);
                subscriber.onNext(instance);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    @SuppressWarnings("unchecked")
    private <T extends SQLiteObject> Observable<T> dbInsert(final @NonNull Context context) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                final SQLiteDAO<T> wrapper = getWrappingObject(
                        (Class<T>)SQLiteObject.this.getClass());
                wrapper.insert(context);
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
                final SQLiteDAO<T> wrapper = getWrappingObject(
                        (Class<T>)SQLiteObject.this.getClass());
                wrapper.update(context);
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

    public void delete(final @NonNull Context context) {
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final SQLiteDAO<?> wrapper = getWrappingObject(SQLiteObject.this.getClass());
                wrapper.delete(context);
                subscriber.onNext(null);
            }
        });
    }

    protected enum ObjectState {
        NEW,
        EXISTING
    }
}