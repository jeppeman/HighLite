package com.jeppeman.sqliteprocessor;

import android.content.Context;

import java.util.List;

/**
 * Interface implemented by generated DAO classes to enable to enable the {@link SQLiteOperator}
 * to call the methods without invoking via reflection
 *
 * @author jesper
 */
public interface SQLiteDAO<T> {
    void insert(Context context);
    void update(Context context);
    void delete(Context context);
    T get(Context context, Object id);
    T get(Context context, String rawQueryClause, String[] rawQueryArgs);
    T get(Context context,
          String whereClause,
          String[] whereArgs,
          String groupBy,
          String having,
          String orderBy);
    List<T> getList(Context context, String rawQueryClause, String[] rawQueryArgs);
    List<T> getList(Context context,
                    String whereClause,
                    String[] whereArgs,
                    String groupBy,
                    String having,
                    String orderBy,
                    String limit);
}