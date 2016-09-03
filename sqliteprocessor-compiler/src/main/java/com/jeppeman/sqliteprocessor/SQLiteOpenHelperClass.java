package com.jeppeman.sqliteprocessor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

/**
 * Generator of subclasses to {@link SQLiteOpenHelper} with the ability to automatically create and
 * update tables described in classes annotated with {@link SQLiteTable}
 *
 * @author jeppeman
 */
final class SQLiteOpenHelperClass extends JavaWritableClass {

    private String mPackageName = "";
    private final String mDatabaseName;
    private final Map<SQLiteTable, Element> mTableElementMap;
    private final Elements mElementUtils;
    private final int mVersion;

    SQLiteOpenHelperClass(final String databaseName,
                          final Map<SQLiteTable, Element> tableElementMap,
                          final int version,
                          final Elements elementUtils) {
        mDatabaseName = databaseName;
        mTableElementMap = tableElementMap;
        mVersion = version;
        mElementUtils = elementUtils;
    }

    private CodeBlock getCreateBlock(final Element element, final SQLiteTable table) {
        return table.autoCreate()
                ? CodeBlock.of("database.execSQL($S);\n",
                getCreateStatement(element, table))
                : CodeBlock.of("");
    }

    private CodeBlock getUpgradeBlock(final Element element, final SQLiteTable table) {
        final String cursorVarName = table.tableName() + "Cursor",
                dbColsVarName = table.tableName() + "Cols";

        final CodeBlock.Builder addColumnStatements = CodeBlock.builder();
        if (table.autoAddColumns()) {
            for (final Element enclosed : element.getEnclosedElements()) {
                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
                if (field == null) continue;

                addColumnStatements.beginControlFlow("if (!$L.contains($S))", dbColsVarName,
                        getDBFieldName(enclosed, field))
                        .addStatement("database.execSQL($S)", getAddColumnStatement(enclosed, field,
                                table))
                        .endControlFlow()
                        .add("\n");
            }
        }

        final CodeBlock.Builder dropColumnStatements = CodeBlock.builder();
        if (table.autoDeleteColumns()) {
            final StringBuilder columnsToSave = new StringBuilder(),
                    columnsToSaveWithTypes = new StringBuilder();
            for (final Element enclosed : element.getEnclosedElements()) {
                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
                if (field == null) continue;

                columnsToSaveWithTypes.append(getDBFieldName(enclosed, field));
                columnsToSaveWithTypes.append(" ");
                columnsToSaveWithTypes.append(getFieldType(enclosed, field));

                final PrimaryKey primaryKey = enclosed.getAnnotation(PrimaryKey.class);
                if (primaryKey != null) {
                    columnsToSaveWithTypes.append(" PRIMARY KEY");
                    columnsToSaveWithTypes.append(
                            enclosed.getAnnotation(AutoIncrement.class) != null
                                    ? " AUTOINCREMENT"
                                    : "");
                }

                columnsToSaveWithTypes.append(", ");

                columnsToSave.append(getDBFieldName(enclosed, field));
                columnsToSave.append(",");
            }
            dropColumnStatements.add(
                    getRecreateTableStatement("database", table.tableName(),
                            columnsToSave.substring(0, columnsToSave.length() - 1),
                            columnsToSaveWithTypes.substring(0,
                                    columnsToSaveWithTypes.length() - 2)));
        }

        return CodeBlock.builder()
                .add(getCreateBlock(element, table))
                .addStatement("final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", null)",
                        CURSOR, cursorVarName, table.tableName())
                .add("\n")
                .addStatement("if (!$L.moveToFirst()) return", cursorVarName)
                .add("\n")
                .addStatement("final $T<String> $L = new $T<>()", LIST, dbColsVarName, ARRAY_LIST)
                .add("\n")
                .beginControlFlow("do")
                .addStatement("$L.add($L.getString(COL_NAME_INDEX))", dbColsVarName,
                        cursorVarName)
                .endControlFlow("while ($L.moveToNext())", cursorVarName)
                .add("\n")
                .addStatement("$L.close()", cursorVarName)
                .add("\n")
                .add(addColumnStatements.build())
                .add(dropColumnStatements.build())
                .build();
    }

    private String getAddColumnStatement(final Element element,
                                         final SQLiteField field,
                                         final SQLiteTable table) {
        final PrimaryKey primaryKey = element.getAnnotation(PrimaryKey.class);
        final StringBuilder builder = new StringBuilder("");
        if (primaryKey != null) {
            builder.append(" PRIMARY KEY");
            builder.append(element.getAnnotation(AutoIncrement.class) != null
                    ? " AUTOINCREMENT" : "");
        }

        return "ALTER TABLE `"
                + table.tableName()
                + "` ADD COLUMN `"
                + getDBFieldName(element, field)
                + "` "
                + getFieldType(element, field)
                + builder.toString()
                + ";";
    }

    private CodeBlock getRecreateTableStatement(final String dbVarName,
                                                final String tableName,
                                                final String columnsToSave,
                                                final String columnsToSaveWithTypes) {
        return CodeBlock.builder()
                .addStatement("$L.execSQL($S)", dbVarName, "BEGIN TRANSACTION;")
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("CREATE TABLE %s_backup(%s);", tableName,
                                columnsToSaveWithTypes))
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("INSERT INTO %s_backup SELECT %s FROM %s;", tableName,
                                columnsToSave, tableName))
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("DROP TABLE %s;", tableName))
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("ALTER TABLE %s_backup RENAME TO %s;", tableName, tableName))
                .addStatement("$L.execSQL($S)", dbVarName, "COMMIT;")
                .build();
    }

    private String getCreateStatement(final Element element, final SQLiteTable table) {
        final StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(table.tableName())
                .append(" (");

        for (final Element enclosed : element.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            builder.append(getDBFieldName(enclosed, field));
            builder.append(" ");
            builder.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = enclosed.getAnnotation(PrimaryKey.class);
            if (primaryKey != null) {
                builder.append(" PRIMARY KEY");
                builder.append(enclosed.getAnnotation(AutoIncrement.class) != null
                        ? " AUTOINCREMENT" : "");
            }

            builder.append(", ");
        }

        return builder.substring(0, builder.length() - 2) + ");";
    }

    private FieldSpec buildColNameIndexField() {
        return FieldSpec.builder(TypeName.INT,
                "COL_NAME_INDEX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("1")
                .build();
    }

    private FieldSpec buildDbNameField() {
        return FieldSpec.builder(STRING, "DATABASE_NAME", Modifier.PRIVATE,
                Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", mDatabaseName)
                .build();
    }

    private FieldSpec buildDbVersionField() {
        return FieldSpec.builder(TypeName.INT,
                "DATABASE_VERSION", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", mVersion)
                .build();
    }

    private MethodSpec buildCtor() {
        return  MethodSpec.constructorBuilder()
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("super(context, $L, null, $L)", "DATABASE_NAME", "DATABASE_VERSION")
                .build();
    }

    private MethodSpec buildOnCreateMethod() {
        final CodeBlock.Builder code = CodeBlock.builder();

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry
                : mTableElementMap.entrySet()) {

            final SQLiteTable table = tableElementEntry.getKey();
            final Element element = tableElementEntry.getValue();

            if (mPackageName.length() == 0) {
                mPackageName = mElementUtils
                        .getPackageOf(element)
                        .getQualifiedName()
                        .toString();
            }

            for (final Element enclosed : element.getEnclosedElements()) {

            }

            code.add(getCreateBlock(element, table));
        }

        return MethodSpec.methodBuilder("onCreate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addCode(code.build())
                .build();
    }

    private MethodSpec buildOnUpgradeMethod() {
        final CodeBlock.Builder code = CodeBlock.builder();

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry
                : mTableElementMap.entrySet()) {

            final SQLiteTable table = tableElementEntry.getKey();
            final Element element = tableElementEntry.getValue();

            if (mPackageName.length() == 0) {
                mPackageName = mElementUtils
                        .getPackageOf(element)
                        .getQualifiedName()
                        .toString();
            }

            code.add(getUpgradeBlock(element, table));
        }

        return MethodSpec.methodBuilder("onUpgrade")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addParameter(TypeName.INT, "oldVersion", Modifier.FINAL)
                .addParameter(TypeName.INT, "newVersion", Modifier.FINAL)
                .addCode(code.build())
                .build();
    }

    @Override
    public JavaFile writeJava() {
        final String className = String.valueOf(mDatabaseName.charAt(0)).toUpperCase()
                + mDatabaseName.substring(1);
        final TypeSpec typeSpec = TypeSpec.classBuilder(
                className + "Helper")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(SQLITE_OPEN_HELPER)
                .addFields(Arrays.asList(
                        buildColNameIndexField(),
                        buildDbNameField(),
                        buildDbVersionField()
                ))
                .addMethods(Arrays.asList(
                        buildCtor(),
                        buildOnCreateMethod(),
                        buildOnUpgradeMethod()
                ))
                .build();

        return JavaFile.builder(mPackageName, typeSpec)
                .addFileComment("Generated code from SQLiteProcessor. Do not modify!")
                .build();
    }
}