package com.jeppeman.sqliteprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * @author jesper
 */
abstract class JavaWritableClass {
    protected static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);
    protected static final ClassName RUNTIME_EXCEPTION = ClassName.get(RuntimeException.class);
    protected static final ClassName BYTE_ARRAY_OS = ClassName.get(ByteArrayOutputStream.class);
    protected static final ClassName BYTE_ARRAY_IS = ClassName.get(ByteArrayInputStream.class);
    protected static final ClassName OBJECT_OS = ClassName.get(ObjectOutputStream.class);
    protected static final ClassName OBJECT_IS = ClassName.get(ObjectInputStream.class);
    protected static final ClassName STRING = ClassName.get(String.class);
    protected static final ClassName STRING_BUILDER = ClassName.get(StringBuilder.class);
    protected static final ClassName LIST = ClassName.get(List.class);
    protected static final ClassName MAP = ClassName.get(Map.class);
    protected static final ClassName HASHMAP = ClassName.get(HashMap.class);
    protected static final ClassName LINKED_HASHMAP = ClassName.get(LinkedHashMap.class);
    protected static final ClassName ARRAY_LIST = ClassName.get(ArrayList.class);
    protected static final ClassName CONTEXT = ClassName.get("android.content", "Context");
    protected static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    protected static final ClassName CONTENT_VALUES = ClassName.get("android.content",
            "ContentValues");
    protected static final ClassName SQLITE_DAO = ClassName.get("com.jeppeman.sqliteprocessor",
            "SQLiteDAO");
    protected static final ClassName SQLITE_DATABASE = ClassName.get("android.database.sqlite",
            "SQLiteDatabase");
    protected static final ClassName SQLITE_OPEN_HELPER = ClassName.get("android.database.sqlite",
            "SQLiteOpenHelper");
    protected static final ClassName COLUMN_META = ClassName.get("com.jeppeman.sqliteprocessor",
            "ColumnMeta");
    protected static final ClassName CLASS_NOT_FOUND_EXCEPTION =
            ClassName.get(ClassNotFoundException.class);

    protected static final Map<SQLiteFieldType, List<Class<?>>> SQLITE_FIELD_CLASS_MAPPING;

    static {
        SQLITE_FIELD_CLASS_MAPPING = new HashMap<>();
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.INTEGER,
                new ArrayList<Class<?>>(Arrays.asList(
                        boolean.class,
                        int.class,
                        short.class,
                        long.class,
                        Boolean.class,
                        Integer.class,
                        Short.class,
                        Long.class)));
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.REAL,
                new ArrayList<Class<?>>(Arrays.asList(
                        float.class,
                        double.class,
                        Float.class,
                        Double.class)));
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.TEXT, new ArrayList<Class<?>>(
                Collections.singletonList(String.class)));
    }

    protected static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    protected String getDBFieldName(final Element element, final SQLiteField field) {
        return field.value() == null || field.value().length() == 0
                ? element.getSimpleName().toString()
                : field.value();
    }

    protected SQLiteFieldType getFieldTypeFromClass(final String cls) {
        SQLiteFieldType ret = SQLiteFieldType.BLOB;
        for (final Map.Entry<SQLiteFieldType, List<Class<?>>> entry
                : SQLITE_FIELD_CLASS_MAPPING.entrySet()) {

            for (final Class<?> clazz : entry.getValue()) {
                if (clazz.getName().equals(cls)) {
                    ret = entry.getKey();
                    break;
                }
            }

            if (ret != SQLiteFieldType.BLOB) break;
        }

        return ret;
    }

    protected String getFieldType(final Element element, final SQLiteField field) {
        try {
            return field.fieldType() != SQLiteFieldType.UNSPECIFIED
                    ? field.fieldType().toString()
                    : getFieldTypeFromClass(element.asType().toString()).toString();
        } catch (Exception e) {
            return null;
        }
    }

    abstract JavaFile writeJava();
}