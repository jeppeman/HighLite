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

    private SQLiteOperator(final @NonNull Context context, final @NonNull Class<T> cls) {
        mClass = cls;
        mContext = context;
    }

    public static <T> SQLiteOperator<T> from(final @NonNull Context context,
                                             final @NonNull Class<T> cls) {
        return new SQLiteOperator<>(context, cls);
    }

    private SQLiteDAO<T> getGeneratedObject(final @Nullable T generator) {
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
    public GetSingleOperation<T> getSingle(final @Nullable Object id) {
        return new GetSingleOperation<>(mContext, getGeneratedObject(null), id);
    }

    public GetSingleOperation<T> getSingle() {
        return getSingle(null);
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
    public GetListOperation<T> getList() {
        return new GetListOperation<>(mContext, getGeneratedObject(null));
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
    public InsertOperation<T> insert(final @NonNull T... objectsToInsert) {
        final SQLiteDAO<T>[] generatedObjects = new SQLiteDAO[objectsToInsert.length];
        for (int i = 0; i < objectsToInsert.length; i++) {
            generatedObjects[i] = getGeneratedObject(objectsToInsert[i]);
        }
        return new InsertOperation<>(mContext, generatedObjects);
    }

    /**
     * Updates a database record based on an object of type {@link T}, blocking operation.
     *
     * @param objectsToUpdate the object to update
     * @return an {@link UpdateOperation}
     */
    public UpdateOperation<T> update(final @Nullable T... objectsToUpdate) {
        final SQLiteDAO generated = getGeneratedObject(null);
        SQLiteDAO<T>[] generatedObjects = null;
        if (objectsToUpdate != null) {
            generatedObjects = new SQLiteDAO[objectsToUpdate.length];
            for (int i = 0; i < objectsToUpdate.length; i++) {
                generatedObjects[i] = getGeneratedObject(objectsToUpdate[i]);
            }
        }

        return new UpdateOperation<>(mContext, generated, generatedObjects);
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
    public DeleteOperation<T> delete(final @Nullable T... objectsToDelete) {
        final SQLiteDAO generated = getGeneratedObject(null);
        SQLiteDAO<T>[] generatedObjects = null;
        if (objectsToDelete != null) {
            generatedObjects = new SQLiteDAO[objectsToDelete.length];
            for (int i = 0; i < objectsToDelete.length; i++) {
                generatedObjects[i] = getGeneratedObject(objectsToDelete[i]);
            }
        }

        return new DeleteOperation<>(mContext, generated, generatedObjects);
    }
}