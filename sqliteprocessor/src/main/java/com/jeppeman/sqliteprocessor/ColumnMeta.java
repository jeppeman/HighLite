package com.jeppeman.sqliteprocessor;

import android.support.annotation.NonNull;

public final class ColumnMeta {
    public final String name;
    public final String type;
    public final boolean isPrimaryKey;
    public final boolean isAutoIncrement;
    public boolean isForeignKey;
    public boolean cascadeOnUpdate;
    public boolean cascadeOnDelete;
    public String tableReference;
    public String fieldReference;

    public ColumnMeta(final @NonNull String name,
                      final @NonNull String type,
                      final boolean isPrimaryKey,
                      final boolean isAutoIncrement) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
    }

    public void setForeignKey(final @NonNull String tableReference,
                              final @NonNull String fieldReference,
                              final boolean cascadeOnDelete,
                              final boolean cascadeOnUpdate) {
        isForeignKey = true;
        this.tableReference = tableReference;
        this.fieldReference = fieldReference;
        this.cascadeOnDelete = cascadeOnDelete;
        this.cascadeOnUpdate = cascadeOnUpdate;
    }

    @Override
    public String toString() {
        return "`" + name + "` " + type + (isPrimaryKey ? " PRIMARY KEY" : "")
                + (isAutoIncrement ? " AUTOINCREMENT" : "")
                + (isForeignKey
                ? ", FOREIGN KEY(`" + name + "`) REFERENCES(`" + fieldReference + "`)"
                + (cascadeOnDelete ? " CASCADE ON DELETE" : "")
                + (cascadeOnUpdate ? " CASCADE ON UPDATE" : "") : "");
    }
}