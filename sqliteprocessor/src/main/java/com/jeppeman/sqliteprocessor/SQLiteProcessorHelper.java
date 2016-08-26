package com.jeppeman.sqliteprocessor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jesper on 2016-08-26.
 */
public abstract class SQLiteProcessorHelper extends SQLiteOpenHelper {

    private static final Map<Class<?>, Constructor<? extends SQLiteOpenHelperCallbacks>>
            CTOR_CACHE = new LinkedHashMap<>();

    protected SQLiteProcessorHelper(final @NonNull Context context,
                                    final @NonNull String name,
                                    final int version) {
        this(context, name, null, version);
    }

    protected SQLiteProcessorHelper(final @NonNull Context context,
                                    final @NonNull String name,
                                    final @Nullable SQLiteDatabase.CursorFactory factory,
                                    final int version) {
        super(context, name, factory, version);
    }

    @SuppressWarnings("unchecked")
    private Constructor<? extends SQLiteOpenHelperCallbacks> getGeneratedConstructor() {
        Constructor<? extends SQLiteOpenHelperCallbacks> ctor = CTOR_CACHE.get(getClass());
        if (ctor != null) return ctor;

        Class<? extends SQLiteOpenHelperCallbacks> cls = null;
        try {
            cls = (Class<? extends SQLiteOpenHelperCallbacks>)
                    Class.forName(getClass().getName() + "_Generated");
            ctor = cls.getConstructor();
            CTOR_CACHE.put(getClass(), ctor);
            return ctor;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to resolve constructor for " + cls.getName(), e);
        }
    }


    @Override
    public void onCreate(final @NonNull SQLiteDatabase sqLiteDatabase) {
        final Constructor<? extends SQLiteOpenHelperCallbacks> ctor = getGeneratedConstructor();
        try {
            ctor.newInstance().onCreate(sqLiteDatabase);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + ctor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke " + ctor, e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create generated helper instance.", cause);
        }
    }

    @Override
    public void onUpgrade(final @NonNull SQLiteDatabase sqLiteDatabase,
                          final int oldVersion,
                          final int newVersion) {
        final Constructor<? extends SQLiteOpenHelperCallbacks> ctor = getGeneratedConstructor();
        try {
            ctor.newInstance().onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + ctor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke " + ctor, e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create generated helper instance.", cause);
        }
    }
}