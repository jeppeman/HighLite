package com.jeppeman.liteomatic;

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
    int update(Context context);
    int updateByQuery(Context context, String whereClause, String[] whereArgs);
    int delete(Context context);
    int deleteByQuery(Context context, String whereClause, String[] whereArgs);
    T getSingle(Context context, Object id);
    T getSingle(Context context, String rawQueryClause, String[] rawQueryArgs);
    T getSingle(Context context,
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