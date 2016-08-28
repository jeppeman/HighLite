package com.jeppeman.sqliteprocessor;

import android.content.Context;

import java.util.List;

/**
 * @author jesper
 */
public interface SQLiteDAO<T extends SQLiteObject> {
    void insert(Context context);
    void update(Context context);
    void delete(Context context);
    void query(Context context);
    T getSingle(Context context, Object id);
    List<T> getCustom(Context context, String customWhere);
}