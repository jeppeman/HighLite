package com.jeppeman.liteomatic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs database operations by delegating calls to a generated DAO. Operations can
 * be blocking or non-blocking returning {@link rx.Observable}s, {@link rx.Single}s or
 * {@link rx.Completable}s
 *
 * @author jesper
 */
@SuppressWarnings({"unchecked", "unused"})
public final class SQLiteOperator<T> {

    private static final Map<Class<?>, Constructor> DAO_CTOR_CACHE = new LinkedHashMap<>();
    private static final Map<Class<?>, SQLiteOpenHelper> HELPER_CACHE = new LinkedHashMap<>();

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

    public static SQLiteDatabase getReadableDatabase(final @NonNull Context context,
                                                     final @NonNull Class<?> cls) {
        return getGeneratedHelper(context, cls).getReadableDatabase();
    }

    public static SQLiteDatabase getWritableDatabase(final @NonNull Context context,
                                                     final @NonNull Class<?> cls) {
        return getGeneratedHelper(context, cls).getWritableDatabase();
    }

    private static SQLiteOpenHelper getGeneratedHelper(final @NonNull Context context,
                                                       final @NonNull Class<?> cls) {
        SQLiteOpenHelper helper;
        try {
            helper = HELPER_CACHE.get(cls);
            if (helper != null) return helper;

            final Class<? extends SQLiteOpenHelper> clazz = (Class<? extends SQLiteOpenHelper>)
                    Class.forName(cls.getCanonicalName() + "_OpenHelper");

            helper = (SQLiteOpenHelper) clazz.getMethod("getInstance", Context.class)
                    .invoke(null, context);
            HELPER_CACHE.put(cls, helper);

            return helper;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access, unable to invoke getInstance method ", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Generated helper class not found", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find method getInstance for " + cls.getName()
                    + "_OpenHelper", e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create Helper instance.", cause);
        }
    }

    private SQLiteDAO<T> getGeneratedDAO(final @Nullable T generator) {
        Constructor<SQLiteDAO<T>> generatedCtor = null;
        try {
            generatedCtor = DAO_CTOR_CACHE.get(mClass);
            if (generatedCtor != null) return generatedCtor.newInstance(generator);

            final Class<SQLiteDAO<T>> clazz = (Class<SQLiteDAO<T>>)
                    Class.forName(mClass.getCanonicalName() + "_DAO");

            generatedCtor = clazz.getConstructor(mClass);
            DAO_CTOR_CACHE.put(mClass, generatedCtor);

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
            throw new RuntimeException("Unable to create DAO instance.", cause);
        }
    }

    /**
     * Generates an executable getSingle operation which fetches a record from a table and maps it
     * to an object of type {@link T}.
     *
     * @param id the id of the object to be returned by the operation. This corresponds to the ID
     *           of a table record.
     * @return an executable {@link GetSingleOperation<T>}
     */
    public GetSingleOperation<T> getSingle(final @Nullable Object id) {
        return new GetSingleOperation<>(mContext, getGeneratedDAO(null), id);
    }

    /**
     * Generates an executable getSingle operation which fetches a record from a table and maps it
     * to an object of type {@link T}. This method requires the returned {@link GetSingleOperation}
     * to have a query specified.
     *
     * @return an executable {@link GetSingleOperation<T>}
     */
    public GetSingleOperation<T> getSingle() {
        return getSingle(null);
    }

    /**
     * Generates an executable getList operation which fetches one or more records from a table and
     * maps them to objects of type {@link T}.
     *
     * @return an executable {@link GetListOperation<T>}
     */
    public GetListOperation<T> getList() {
        return new GetListOperation<>(mContext, getGeneratedDAO(null));
    }

    /**
     * Generates an executable save operation which inserts or updates one or more records into a
     * table where fields are mapped from the type {@link T}.
     *
     * @param objectsToInsert the objects to insert.
     * @return an executable {@link SaveOperation<T>}
     */
    public SaveOperation<T> save(final @NonNull T... objectsToInsert) {
        final SQLiteDAO generated = getGeneratedDAO(null);
        final SQLiteDAO<T>[] generatedObjects = new SQLiteDAO[objectsToInsert.length];
        for (int i = 0; i < objectsToInsert.length; i++) {
            generatedObjects[i] = getGeneratedDAO(objectsToInsert[i]);
        }
        return new SaveOperation<>(mContext, generated, generatedObjects);
    }

    public SaveOperation<T> save(final @NonNull List<T> objectsToInsert) {
        return save((T[]) objectsToInsert.toArray());
    }

    /**
     * Generates an executable delete operation which deletes one or more records from a table where
     * based on the ID:s of the objects passed as parameters. If no objects are given as parameters
     * a query has to be specified.
     *
     * @param objectsToDelete the objects to delete.
     * @return an executable {@link DeleteOperation<T>}
     */
    public DeleteOperation<T> delete(final @Nullable T... objectsToDelete) {
        final SQLiteDAO generated = getGeneratedDAO(null);
        SQLiteDAO<T>[] generatedObjects = null;
        if (objectsToDelete != null) {
            generatedObjects = new SQLiteDAO[objectsToDelete.length];
            for (int i = 0; i < objectsToDelete.length; i++) {
                generatedObjects[i] = getGeneratedDAO(objectsToDelete[i]);
            }
        }

        return new DeleteOperation<>(mContext, generated, generatedObjects);
    }

    public DeleteOperation<T> delete(final @NonNull List<T> objectsToDelete) {
        return delete((T[]) objectsToDelete.toArray());
    }
}