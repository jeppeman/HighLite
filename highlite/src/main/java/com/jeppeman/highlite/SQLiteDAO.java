package com.jeppeman.highlite;

import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * Interface implemented by generated DAO classes to enable to enable the {@link SQLiteOperator}
 * to call the methods without invoking via reflection
 *
 * @author jesper
 */
public interface SQLiteDAO<T> {
    int save(Context context);
    int saveByQuery(Context context,
                    Map<String, Object> colsToSave,
                    String whereClause,
                    String[] whereArgs);
    int delete(Context context);
    int deleteByQuery(Context context, String whereClause, String[] whereArgs);
    T getSingle(Context context,
                Object id,
                boolean fetchForeignKeys,
                boolean fetchRelationShips);
    T getSingle(Context context,
                String rawQueryClause,
                String[] rawQueryArgs,
                boolean fetchForeignKeys,
                boolean fetchRelationShips,
                boolean fromCache);
    T getSingle(Context context,
                String whereClause,
                String[] whereArgs,
                String groupBy,
                String having,
                String orderBy,
                boolean fetchForeignKeys,
                boolean fetchRelationShips,
                boolean fromCache);
    List<T> getList(Context context,
                    String rawQueryClause,
                    String[] rawQueryArgs,
                    boolean fetchForeignKeys,
                    boolean fetchRelationShips,
                    boolean fromCache);
    List<T> getList(Context context,
                    String whereClause,
                    String[] whereArgs,
                    String groupBy,
                    String having,
                    String orderBy,
                    String limit,
                    boolean fetchForeignKeys,
                    boolean fetchRelationShips,
                    boolean fromCache);
}