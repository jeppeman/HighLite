package com.jeppeman.sqliteprocessor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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
    private final Types mTypeUtils;
    private final int mVersion;

    SQLiteOpenHelperClass(final String databaseName,
                          final Map<SQLiteTable, Element> tableElementMap,
                          final int version,
                          final Elements elementUtils,
                          final Types typeUtils) {
        mDatabaseName = databaseName;
        mTableElementMap = tableElementMap;
        mVersion = version;
        mElementUtils = elementUtils;
        mTypeUtils = typeUtils;
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

//        final CodeBlock.Builder addColumnStatements = CodeBlock.builder();
//        if (table.autoAddColumns()) {
//            addColumnStatements
//                    .addStatement("final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", "
//                            + "null)", CURSOR, cursorVarName, table.tableName())
//                    .add("\n")
//                    .addStatement("if (!$L.moveToFirst()) return", cursorVarName)
//                    .add("\n")
//                    .addStatement("final $T<String> $L = new $T<>()", LIST, dbColsVarName,
//                            ARRAY_LIST)
//                    .add("\n")
//                    .beginControlFlow("do")
//                    .addStatement("$L.add($L.getString(COL_NAME_INDEX))", dbColsVarName,
//                            cursorVarName)
//                    .endControlFlow("while ($L.moveToNext())", cursorVarName)
//                    .add("\n")
//                    .addStatement("$L.close()", cursorVarName)
//                    .add("\n");
//            for (final Element enclosed : element.getEnclosedElements()) {
//                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
//                if (field == null) continue;
//
//                addColumnStatements.beginControlFlow("if (!$L.contains($S))", dbColsVarName,
//                        getDBFieldName(enclosed, field))
//                        .addStatement("database.execSQL($S)", getAddColumnStatement(enclosed,
// field,
//                                table))
//                        .endControlFlow()
//                        .add("\n");
//            }
//        }

        final CodeBlock.Builder recreateStatement = CodeBlock.builder();
        if (table.autoAddColumns() && table.autoDeleteColumns()) {
            final StringBuilder columnsToSave = new StringBuilder(),
                    columnsToSaveWithTypes = new StringBuilder();
            for (final Element enclosed : element.getEnclosedElements()) {
                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
                if (field == null) continue;

                final String fieldName = getDBFieldName(enclosed, field);
                columnsToSaveWithTypes.append("`");
                columnsToSaveWithTypes.append(fieldName);
                columnsToSaveWithTypes.append("`");
                columnsToSaveWithTypes.append(" ");
                columnsToSaveWithTypes.append(getFieldType(enclosed, field));

                final PrimaryKey primaryKey = enclosed.getAnnotation(PrimaryKey.class);
                if (primaryKey != null) {
                    columnsToSaveWithTypes.append(" PRIMARY KEY");
                    columnsToSaveWithTypes.append(
                            primaryKey.autoIncrement()
                                    ? " AUTOINCREMENT"
                                    : "");
                }

                final ForeignKey foreignKey = enclosed.getAnnotation(ForeignKey.class);
                if (foreignKey != null) {
                    columnsToSaveWithTypes.append(", ");
                    columnsToSaveWithTypes.append(
                            String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                                    fieldName, foreignKey.table(), foreignKey.fieldReference()));
                }

                columnsToSaveWithTypes.append(", ");

                columnsToSave.append(getDBFieldName(enclosed, field));
                columnsToSave.append(",");
            }
            recreateStatement.add(
                    getRecreateTableStatement("database", table.tableName(),
                            columnsToSave.substring(0, columnsToSave.length() - 1),
                            columnsToSaveWithTypes.substring(0,
                                    columnsToSaveWithTypes.length() - 2)))
                    .add("\n");
        } else if (!table.autoDeleteColumns()) {
            final String colsWithTypesVar = table.tableName() + "colsWithTypesBuilder",
                    colsVar = table.tableName() + "ColsBuilder";
            recreateStatement
                    .addStatement("final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", "
                            + "null)", CURSOR, cursorVarName, table.tableName())
                    .add("\n")
                    .addStatement("if (!$L.moveToFirst()) return", cursorVarName)
                    .add("\n")
                    .addStatement("final $T<String, String> $L = new $T<>()", MAP, dbColsVarName,
                            LINKED_HASHMAP)
                    .add("\n")
                    .beginControlFlow("do")
                    .addStatement("$L.put($L.getString(COL_NAME_INDEX), "
                                    + "$L.getString(COL_TYPE_INDEX))", dbColsVarName, cursorVarName,
                            cursorVarName)
                    .endControlFlow("while ($L.moveToNext())", cursorVarName)
                    .add("\n")
                    .addStatement("$L.close()", cursorVarName)
                    .add("\n")
                    .add("final StringBuilder $L = new StringBuilder(), ", colsWithTypesVar)
                    .add("$L = new StringBuilder();\n", colsVar)
                    .beginControlFlow("for (final Map.Entry<String, String> entry : $L.entrySet())",
                            dbColsVarName)
                    .addStatement("$L.append(String.format(\"`%s` %s\", entry.getKey(), "
                            + "entry.getValue()))", colsWithTypesVar)
                    .addStatement("$L.append($S)", colsWithTypesVar, ", ")
                    .addStatement("$L.append(String.format(\"`%s`\", entry.getKey()))", colsVar)
                    .addStatement("$L.append($S)", colsVar, ", ")
                    .endControlFlow();

            recreateStatement
                    .add(getRecreateTableStatement("database", table.tableName()))
                    .add("\n");
        }

        return CodeBlock.builder()
                .add(getCreateBlock(element, table))
                .add(recreateStatement.build())
                .build();
    }

//    private String getAddColumnStatement(final Element element,
//                                         final SQLiteField field,
//                                         final SQLiteTable table) {
//        final PrimaryKey primaryKey = element.getAnnotation(PrimaryKey.class);
//        final StringBuilder builder = new StringBuilder("");
//        if (primaryKey != null) {
//            builder.append(" PRIMARY KEY");
//            builder.append(primaryKey.autoIncrement()
//                    ? " AUTOINCREMENT" : "");
//        }
//
//        return "ALTER TABLE `"
//                + table.tableName()
//                + "` ADD COLUMN `"
//                + getDBFieldName(element, field)
//                + "` "
//                + getFieldType(element, field)
//                + builder.toString()
//                + ";";
//    }

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

    private CodeBlock getRecreateTableStatement(final String dbVarName, final String tableName) {
        return CodeBlock.builder()
                .addStatement("$L.execSQL($S)", dbVarName, "BEGIN TRANSACTION;")
                .addStatement("$L.execSQL(String.format(\"CREATE TABLE $L_backup($L);\", "
                                + "$LcolsWithTypesBuilder.toString()))", dbVarName,
                        tableName, "%s", tableName)
                .addStatement("$L.execSQL(String.format(\"INSERT INTO $L_backup SELECT "
                                + "%s FROM $L;\", $LColsBuilder.toString()))", dbVarName,
                        tableName, tableName, tableName)
//                        String.format(CodeBlock.of("").toString(), tableName, tableName))
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

            final String fieldName = getDBFieldName(enclosed, field);
            builder.append("`");
            builder.append(fieldName);
            builder.append("`");
            builder.append(" ");
            builder.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = enclosed.getAnnotation(PrimaryKey.class);
            if (primaryKey != null) {
                builder.append(" PRIMARY KEY");
                builder.append(primaryKey.autoIncrement()
                        ? " AUTOINCREMENT" : "");
            }

            final ForeignKey foreignKey = enclosed.getAnnotation(ForeignKey.class);
            if (foreignKey != null) {
                builder.append(", ");
                builder.append(String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                        fieldName, foreignKey.table(), foreignKey.fieldReference()));
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

    private FieldSpec buildColTypeIndexField() {
        return FieldSpec.builder(TypeName.INT,
                "COL_TYPE_INDEX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("2")
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
        return MethodSpec.constructorBuilder()
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

            final CodeBlock.Builder onCreateStatements = CodeBlock.builder();
            int onCreateCounter = 0;
            for (final Element enclosed : element.getEnclosedElements()) {
                if (enclosed.getAnnotation(OnCreate.class) == null) continue;

                if (++onCreateCounter > 1) {
                    throw new ProcessingException(enclosed,
                            String.format("Only one method per class may be annotated with %s",
                                    OnCreate.class.getCanonicalName()));
                }

                if (!enclosed.getModifiers().contains(Modifier.STATIC)) {
                    throw new ProcessingException(enclosed,
                            String.format("%s annotated methods need to be static",
                                    OnCreate.class.getCanonicalName()));
                }

                final ExecutableElement executableElement = (ExecutableElement) enclosed;
                if (executableElement.getParameters().size() != 1
                        || !SQLITE_DATABASE.equals(
                        ClassName.get(executableElement.getParameters().get(0).asType()))) {
                    throw new ProcessingException(enclosed,
                            String.format("%s annotated methods needs to have exactly "
                                            + "one parameter, being of type %s",
                                    OnCreate.class.getCanonicalName(),
                                    SQLITE_DATABASE.toString()));
                }

                onCreateStatements.addStatement("$T.$L(database)", ClassName.get(element.asType()),
                        enclosed.getSimpleName());
            }

            code.add(onCreateStatements.build());
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

            final CodeBlock.Builder onUpgradeStatements = CodeBlock.builder();
            int onUpgradeCounter = 0;
            for (final Element enclosed : element.getEnclosedElements()) {
                if (enclosed.getAnnotation(OnUpgrade.class) == null) continue;

                if (++onUpgradeCounter > 1) {
                    throw new ProcessingException(enclosed,
                            String.format("Only one method per class may be annotated with %s",
                                    OnUpgrade.class.getCanonicalName()));
                }

                if (!enclosed.getModifiers().contains(Modifier.STATIC)) {
                    throw new ProcessingException(enclosed,
                            String.format("%s annotated methods need to be static",
                                    OnUpgrade.class.getCanonicalName()));
                }

                final ExecutableElement executableElement = (ExecutableElement) enclosed;
                if (executableElement.getParameters().size() != 3
                        || !SQLITE_DATABASE.equals(
                        ClassName.get(executableElement.getParameters().get(0).asType()))
                        || !TypeName.INT.equals(
                        ClassName.get(executableElement.getParameters().get(1).asType()))
                        || !TypeName.INT.equals(
                        ClassName.get(executableElement.getParameters().get(2).asType()))) {
                    throw new ProcessingException(enclosed,
                            String.format("%s annotated methods needs to have exactly "
                                            + "three parameters, being of type %s, %s and %s "
                                            + "respectively",
                                    OnUpgrade.class.getCanonicalName(),
                                    SQLITE_DATABASE.toString(),
                                    TypeName.INT.toString(),
                                    TypeName.INT.toString()));
                }

                onUpgradeStatements.addStatement("$T.$L(database, oldVersion, newVersion)",
                        ClassName.get(element.asType()), enclosed.getSimpleName());
            }

            code.add(onUpgradeStatements.build());
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
                        buildColTypeIndexField(),
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