package com.jeppeman.sqliteprocessor.compiler;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;
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
 * Created by jesper on 2016-08-26.
 */
final class SQLiteOpenHelperClass extends JavaWritableClass {

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
                        getFieldName(enclosed, field))
                        .addStatement("database.execSQL($S)", getAddColumnStatement(enclosed, field,
                                table))
                        .endControlFlow()
                        .add("\n");
            }
        }

        final CodeBlock.Builder dropColumnStatements = CodeBlock.builder();
        if (table.autoDeleteColumns()) {
            final StringBuilder columnsToSave = new StringBuilder();
            for (final Element enclosed : element.getEnclosedElements()) {
                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
                if (field == null) continue;

                columnsToSave.append(getFieldName(enclosed, field));
                columnsToSave.append(",");
            }
            dropColumnStatements.addStatement("database.execSQL($S)",
                    getRecreateTableStatement(table.tableName(),
                            columnsToSave.substring(0, columnsToSave.length() - 1)));
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
                + getFieldName(element, field)
                + "` "
                + getFieldType(element, field)
                + builder.toString();
    }

    private String getRecreateTableStatement(final String tableName, final String columnsToSave) {
        return "BEGIN TRANSACTION;" +
                String.format("CREATE TEMPORARY TABLE %s_backup(%s);", tableName, columnsToSave) +
                String.format("INSERT INTO %s_backup SELECT %s FROM %s;", tableName, columnsToSave, tableName) +
                String.format("DROP TABLE %s;", tableName) +
                String.format("ALTER TABLE %s_backup RENAME TO %s;", tableName, tableName) +
                "COMMIT;";
    }

    private String getCreateStatement(final Element element, final SQLiteTable table) {
        final StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(table.tableName())
                .append(" (");

        for (final Element enclosed : element.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            builder.append(getFieldName(enclosed, field));
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

    @Override
    public JavaFile writeJava() {
        final String className = String.valueOf(mDatabaseName.charAt(0)).toUpperCase()
                + mDatabaseName.substring(1);

        final FieldSpec colNameIndex = FieldSpec.builder(TypeName.INT,
                "COL_NAME_INDEX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("1")
                .build();

        final FieldSpec dbName = FieldSpec.builder(STRING, "DATABASE_NAME", Modifier.PRIVATE,
                Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", mDatabaseName)
                .build();

        final FieldSpec dbVersion = FieldSpec.builder(TypeName.INT,
                "DATABASE_VERSION", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", mVersion)
                .build();

        final MethodSpec ctor = MethodSpec.constructorBuilder()
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addCode("super(context, $L, null, $L);\n", dbName.name, dbVersion.name)
                .build();

        String packageName = null;
        final CodeBlock.Builder codeBlockOnCreate = CodeBlock.builder(),
                codeBlockOnUpgrade = CodeBlock.builder();

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry :
                mTableElementMap.entrySet()) {

            final SQLiteTable table = tableElementEntry.getKey();
            final Element element = tableElementEntry.getValue();

            if (packageName == null) {
                packageName = mElementUtils
                        .getPackageOf(element)
                        .getQualifiedName()
                        .toString();
            }

            codeBlockOnCreate.add(getCreateBlock(element, table));
            codeBlockOnUpgrade.add(getUpgradeBlock(element, table));
        }

        final MethodSpec onCreateMethod = MethodSpec.methodBuilder("onCreate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addCode(codeBlockOnCreate.build())
                .build();

        final MethodSpec onUpgradeMethod = MethodSpec.methodBuilder("onUpgrade")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addParameter(TypeName.INT, "oldVersion", Modifier.FINAL)
                .addParameter(TypeName.INT, "newVersion", Modifier.FINAL)
                .addCode(codeBlockOnUpgrade.build())
                .build();

        final TypeSpec typeSpec = TypeSpec.classBuilder(
                className + "Helper")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(SQLITE_OPEN_HELPER)
                .addFields(Arrays.asList(colNameIndex, dbVersion, dbName))
                .addMethods(Arrays.asList(ctor, onCreateMethod, onUpgradeMethod))
                .build();

        return JavaFile.builder(packageName, typeSpec)
                .addFileComment("Generated code from SQLiteProcessor. Do not modify!")
                .build();
    }
}