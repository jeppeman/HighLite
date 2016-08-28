package com.jeppeman.sqliteprocessor;

import android.content.Context;

import java.util.List;

/**
 * @author jesper
 */
public interface SQLiteDAO<T> {
    void insert(Context context);
    void update(Context context);
    void delete(Context context);
    T getSingle(Context context, Object id);
    T getSingle(Context context,
                String selectionClause,
                String[] whereArgs,
                String groupBy,
                String having,
                String orderBy);
    List<T> getCustom(Context context,
                      String selectionClause,
                      String[] whereArgs,
                      String groupBy,
                      String having,
                      String orderBy);
}