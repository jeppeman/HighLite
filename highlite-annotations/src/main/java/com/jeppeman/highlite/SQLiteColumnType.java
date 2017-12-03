package com.jeppeman.highlite;

/**
 * Enum representation of SQLite column types
 *
 * @author jesper
 */
public enum SQLiteColumnType {
    /**
     * A suitable SQLite type will try to be found based on the type of the field in question,
     * if none can be found {@link SQLiteColumnType#BLOB} will be used
     */
    UNSPECIFIED,
    /**
     * The TEXT SQLite field type, used by default for {@link String} class fields
     */
    TEXT,
    /**
     * The INTEGER SQLite field type, used by default for class fields of type
     * {@link Short}, {@link Integer} and {@link Long} and their respective primitive counterparts
     */
    INTEGER,
    /**
     * The REAL SQLite field type, used by default for class fields of type
     * {@link Float} and {@link Double} and their respective primitive counterparts
     */
    REAL,
    /**
     * The BLOB SQLite field type, used by default if a match couldn't be found for
     * {@link SQLiteColumnType#TEXT}, {@link SQLiteColumnType#INTEGER} or
     * {@link SQLiteColumnType#REAL}
     */
    BLOB
}
