package com.jeppeman.highlite;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Generator of subclasses to {@link SQLiteOpenHelper} with the ability to automatically create and
 * update tables described in classes annotated with {@link SQLiteTable}
 *
 * @author jeppeman
 */
final class SQLiteOpenHelperClass extends JavaWritableClass {

    private final Element mElement;
    private final String mPackageName;
    private final String mDatabaseName;
    private final Map<SQLiteTable, Element> mTableElementMap;
    private final Elements mElementUtils;
    private final Types mTypeUtils;
    private final int mVersion;

    SQLiteOpenHelperClass(final Element element,
                          final String packageName,
                          final String databaseName,
                          final Map<SQLiteTable, Element> tableElementMap,
                          final int version,
                          final Elements elementUtils,
                          final Types typeUtils) {
        mElement = element;
        mPackageName = packageName;
        mDatabaseName = databaseName;
        mTableElementMap = tableElementMap;
        mVersion = version;
        mElementUtils = elementUtils;
        mTypeUtils = typeUtils;
    }

    private ClassName getHelperClassName() {
        return ClassName.get(mPackageName,
                String.valueOf(mDatabaseName.charAt(0)).toUpperCase()
                        + mDatabaseName.substring(1) + "_OpenHelper");
    }

    private CodeBlock getCreateBlock(final Element element, final SQLiteTable table) {
        return table.autoCreate()
                ? CodeBlock.of("database.execSQL($S);\n", getCreateStatement(element, table))
                : CodeBlock.of("");
    }

    private CodeBlock getInitialRecreationBlock(final SQLiteTable table,
                                                final Map<String, String> columnsMap,
                                                final Map<String, String> foreignKeysMap) {
        final String cursorVarName = table.tableName() + "Cursor",
                dbColsVarName = table.tableName() + "Cols",
                colsToSaveVarName = table.tableName() + "ColsToSave",
                createSqlCursorVarName = table.tableName() + "CreateCursor",
                createSqlStatementVarName = table.tableName() + "Create",
                foreignKeysSplitVarName = table.tableName() + "ForeignKeysSplit",
                foreignKeysVarName = table.tableName() + "ForeignKeys",
                shouldRecreateVarName = table.tableName() + "ShouldRecreate",
                tableExistsVarName = table.tableName() + "Exists";

        final String currentFieldsVar = table.tableName() + "CurrentFields";
        final CodeBlock.Builder currentFieldsPopulator = CodeBlock.builder();
        for (final Map.Entry<String, String> entry : columnsMap.entrySet()) {
            currentFieldsPopulator.addStatement("$L.put($S, new $T[] { $S, $S })",
                    currentFieldsVar, entry.getKey(), STRING, entry.getValue(),
                    foreignKeysMap.containsKey(entry.getKey())
                            ? foreignKeysMap.get(entry.getKey())
                            : "");
        }

        return CodeBlock.builder()
                .add("// Check whether $L exists or not\n", table.tableName())
                .addStatement("boolean $L", tableExistsVarName)
                .addStatement("final $T $L = database.rawQuery(\"SELECT `sql` FROM "
                                + "sqlite_master WHERE `type` = ? AND `name` = ?;\", \n"
                                + "new $T[] { $S, $S })",
                        CURSOR, createSqlCursorVarName, STRING, "table", table.tableName())
                .addStatement("$T $L = $S", STRING, createSqlStatementVarName, "")
                .beginControlFlow("if ($L.moveToFirst())", createSqlCursorVarName)
                .addStatement("$L = true", tableExistsVarName)
                .beginControlFlow("do")
                .addStatement("$L += $L.getString(0)", createSqlStatementVarName,
                        createSqlCursorVarName)
                .endControlFlow("while ($L.moveToNext())", createSqlCursorVarName)
                .nextControlFlow("else")
                .addStatement("$L = false", tableExistsVarName)
                .endControlFlow()
                .addStatement("$L.close()", createSqlCursorVarName)
                .add("\n")
                .beginControlFlow("if ($L) /* $L existed, do column additions / deletions */",
                        tableExistsVarName, table.tableName())
                .add("// Collect columns based on current state of the class\n")
                .addStatement("final $T<String, String[]> $L = new $T<>()", MAP,
                        currentFieldsVar, LINKED_HASHMAP)
                .add(currentFieldsPopulator.build())
                .add("\n")
                .add("// Check which columns are currently in the table\n", table.tableName())
                .addStatement("final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", "
                        + "null)", CURSOR, cursorVarName, table.tableName())
                .addStatement("final $T<$T> $L = new $T<>()", LIST, STRING, dbColsVarName,
                        ARRAY_LIST)
                .add("\n")
                .beginControlFlow("if ($L.moveToFirst())", cursorVarName)
                .beginControlFlow("do")
                .addStatement("$L.add($L.getString(COL_NAME_INDEX))", dbColsVarName,
                        cursorVarName)
                .endControlFlow("while ($L.moveToNext())", cursorVarName)
                .endControlFlow()
                .addStatement("$L.close()", cursorVarName)
                .add("\n")
                .addStatement("$L = $L.substring(0, $L.length() - 1) + $S",
                        createSqlStatementVarName, createSqlStatementVarName,
                        createSqlStatementVarName, ", ")
                .addStatement("$T $L = new $T()", STRING_BUILDER, colsToSaveVarName,
                        STRING_BUILDER)
                .addStatement("$T[] $L = $L.split($S)", STRING, foreignKeysSplitVarName,
                        createSqlStatementVarName, "FOREIGN KEY")
                .addStatement("$L = $L[0]", createSqlStatementVarName, foreignKeysSplitVarName)
                .addStatement("$T $L = $S", STRING, foreignKeysVarName, "")
                .beginControlFlow("for (int i = 1; i < $L.length; i++)",
                        foreignKeysSplitVarName)
                .addStatement("$L += $S + $L[i]", foreignKeysVarName,
                        "FOREIGN KEY", foreignKeysSplitVarName)
                .endControlFlow()
                .addStatement("boolean $L = false", shouldRecreateVarName)
                .build();
    }

    private CodeBlock getUpgradeBlock(final Element element, final SQLiteTable table) {
        final String currentFieldsVar = table.tableName() + "CurrentFields",
                dbColsVarName = table.tableName() + "Cols",
                colsToSaveVarName = table.tableName() + "ColsToSave",
                createSqlStatementVarName = table.tableName() + "Create",
                foreignKeysVarName = table.tableName() + "ForeignKeys",
                shouldRecreateVarName = table.tableName() + "ShouldRecreate",
                tableExistsVarName = table.tableName() + "Exists";

        final CodeBlock.Builder recreateStatement = CodeBlock.builder();
        final Map<String, String> columnsMap = new LinkedHashMap<>(),
                foreignKeysMap = new LinkedHashMap<>();

        for (final Element enclosed : element.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);

            if (field == null) continue;

            final StringBuilder fieldCreator = new StringBuilder();
            final String fieldName = getDBFieldName(enclosed, field);
            fieldCreator.append("`");
            fieldCreator.append(fieldName);
            fieldCreator.append("`");
            fieldCreator.append(" ");
            fieldCreator.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = field.primaryKey();
            if (primaryKey.enabled()) {
                fieldCreator.append(" PRIMARY KEY");
                fieldCreator.append(
                        primaryKey.autoIncrement()
                                ? " AUTOINCREMENT"
                                : "");
            }

            if (field.unique()) {
                fieldCreator.append(" UNIQUE");
            }

            if (field.notNull()) {
                fieldCreator.append(" NOT NULL");
            }

            final ForeignKey foreignKey = field.foreignKey();
            if (foreignKey.enabled()) {
                final Element foreignKeyRefElement = findForeignKeyReferencedField(enclosed,
                        foreignKey);
                final SQLiteTable foreignKeyRefTable = getForeignKeyReferencedTable(foreignKey);
                final StringBuilder foreignKeyBuilder = new StringBuilder();
                foreignKeyBuilder.append(
                        String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                                fieldName, foreignKeyRefTable.tableName(),
                                getDBFieldName(foreignKeyRefElement,
                                        foreignKeyRefElement.getAnnotation(SQLiteField.class))));
                if (foreignKey.cascadeOnDelete()) {
                    foreignKeyBuilder.append(" ON DELETE CASCADE");
                }

                if (foreignKey.cascadeOnUpdate()) {
                    foreignKeyBuilder.append(" ON UPDATE CASCADE");
                }

                foreignKeyBuilder.append(", ");

                foreignKeysMap.put(fieldName, foreignKeyBuilder.toString());
            }

            columnsMap.put(fieldName, fieldCreator.toString());
        }

        final CodeBlock initialRecreationBlock = getInitialRecreationBlock(table, columnsMap,
                foreignKeysMap);

        if (table.autoAddColumns() && table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .addStatement("$L = $S", foreignKeysVarName, "")
                    .addStatement("$L = $S", createSqlStatementVarName, "CREATE TABLE "
                            + table.tableName() + " (")
                    .beginControlFlow("for (final $T.Entry<$T, $T[]> entry : $L.entrySet())", MAP,
                            STRING, STRING, currentFieldsVar)
                    .beginControlFlow("if (!$L.contains(entry.getKey()))", dbColsVarName)
                    .addStatement("$L = true", shouldRecreateVarName)
                    .addStatement("database.execSQL($S + entry.getValue()[0])",
                            "ALTER TABLE `" + table.tableName() + "` ADD COLUMN ")
                    .endControlFlow()
                    .add("\n")
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(entry.getKey())", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .addStatement("$L += entry.getValue()[1]", foreignKeysVarName)
                    .addStatement("$L += entry.getValue()[0] + $S", createSqlStatementVarName, ", ")
                    .endControlFlow()
                    .beginControlFlow("if ($L.length() > 0)", colsToSaveVarName)
                    .addStatement("$L = new $T($L.substring(0, $L.length() - 2))",
                            colsToSaveVarName, STRING_BUILDER, colsToSaveVarName, colsToSaveVarName)
                    .endControlFlow()
                    .addStatement("$L = ($L + $L).substring(0, ($L + $L).length() - 2) + $S",
                            createSqlStatementVarName, createSqlStatementVarName,
                            foreignKeysVarName, createSqlStatementVarName,
                            foreignKeysVarName, ");")
                    .beginControlFlow("if ($L && $L)", shouldRecreateVarName, tableExistsVarName)
                    .add(getRecreateStatement("database", table.tableName(),
                            createSqlStatementVarName, colsToSaveVarName))
                    .endControlFlow()
                    .add("\n");
        } else if (table.autoAddColumns() && !table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .beginControlFlow("for (final $T.Entry<$T, $T[]> entry : $L.entrySet())", MAP,
                            STRING, STRING, currentFieldsVar)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(entry.getKey())", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .beginControlFlow("if ($L.contains(entry.getKey()))", dbColsVarName)
                    .addStatement("$L = $L.replaceAll(\"`?\" + entry.getKey() + \"`?[^,]+\", "
                                    + "entry.getValue()[0])", createSqlStatementVarName,
                            createSqlStatementVarName)
                    .addStatement("$L = true", shouldRecreateVarName)
                    .addStatement("continue")
                    .endControlFlow()
                    .add("\n")
                    .addStatement("$L = true", shouldRecreateVarName)
                    .addStatement("database.execSQL($S + entry.getValue()[0])",
                            "ALTER TABLE `" + table.tableName() + "` ADD COLUMN ")
                    .addStatement("$L += entry.getValue()[1]", foreignKeysVarName)
                    .addStatement("$L += entry.getValue()[0] + $S", createSqlStatementVarName, ", ")
                    .endControlFlow()
                    .beginControlFlow("for (final $T column : $L)", STRING, dbColsVarName)
                    .beginControlFlow("if (!$L.containsKey(column))", currentFieldsVar)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(column)", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("if ($L.length() > 0)", colsToSaveVarName)
                    .addStatement("$L = new $T($L.substring(0, $L.length() - 2))",
                            colsToSaveVarName, STRING_BUILDER, colsToSaveVarName, colsToSaveVarName)
                    .endControlFlow()
                    .addStatement("$L = ($L + $L).substring(0, ($L + $L).length() - 2) + $S",
                            createSqlStatementVarName, createSqlStatementVarName,
                            foreignKeysVarName, createSqlStatementVarName,
                            foreignKeysVarName, ");")
                    .beginControlFlow("if ($L && $L)", shouldRecreateVarName, tableExistsVarName)
                    .add(getRecreateStatement("database", table.tableName(),
                            createSqlStatementVarName, colsToSaveVarName))
                    .endControlFlow()
                    .add("\n");
        } else if (!table.autoAddColumns() && table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .addStatement("$L = $S", foreignKeysVarName, "")
                    .addStatement("$L = $S", createSqlStatementVarName, "CREATE TABLE "
                            + table.tableName() + " (")
                    .beginControlFlow("for (final String column : $L)", dbColsVarName)
                    .beginControlFlow("if ($L.containsKey(column))", currentFieldsVar)
                    .addStatement("$L += $L.get(column)[1]", foreignKeysVarName, currentFieldsVar)
                    .addStatement("$L += $L.get(column)[0] + $S", createSqlStatementVarName,
                            currentFieldsVar, ", ")
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(column)", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .nextControlFlow("else")
                    .addStatement("$L = true", shouldRecreateVarName)
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("if ($L.length() > 0)", colsToSaveVarName)
                    .addStatement("$L = new $T($L.substring(0, $L.length() - 2))",
                            colsToSaveVarName, STRING_BUILDER, colsToSaveVarName, colsToSaveVarName)
                    .endControlFlow()
                    .addStatement("$L = ($L + $L).substring(0, ($L + $L).length() - 2) + $S",
                            createSqlStatementVarName, createSqlStatementVarName,
                            foreignKeysVarName, createSqlStatementVarName,
                            foreignKeysVarName, ");")
                    .beginControlFlow("if ($L && $L)", shouldRecreateVarName, tableExistsVarName)
                    .add(getRecreateStatement("database", table.tableName(),
                            createSqlStatementVarName, colsToSaveVarName))
                    .endControlFlow()
                    .add("\n");
        }

        return CodeBlock.builder()
                .add(recreateStatement.build())
                .build();
    }

    private CodeBlock getRecreateStatement(final String dbVarName,
                                           final String tableName,
                                           final String colsWithTypesVarName,
                                           final String colsVarName) {
        return CodeBlock.builder()
                .addStatement("$L.execSQL($S)", dbVarName, "BEGIN TRANSACTION;")
                .addStatement("$L.execSQL($L.replace($S, $S))", dbVarName, colsWithTypesVarName,
                        "CREATE TABLE " + tableName, "CREATE TABLE " + tableName + "_backup")
                .addStatement("$L.execSQL(String.format(\"INSERT INTO $L_backup SELECT "
                                + "%s FROM $L;\", $L))", dbVarName,
                        tableName, tableName, colsVarName)
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("DROP TABLE %s;", tableName))
                .addStatement("$L.execSQL($S)", dbVarName,
                        String.format("ALTER TABLE %s_backup RENAME TO %s;", tableName, tableName))
                .addStatement("$L.execSQL($S)", dbVarName, "COMMIT;")
                .build();
    }

    private Element findForeignKeyReferencedField(final Element enclosed,
                                                  final ForeignKey foreignKey) {
        TypeMirror mirror = null;
        try {
            foreignKey.table();
        } catch (MirroredTypeException e) {
            mirror = e.getTypeMirror();
        }

        final Element tableElem = mTypeUtils.asElement(mirror);
        if (tableElem.getAnnotation(SQLiteTable.class) == null) {
            throw new ProcessingException(enclosed,
                    String.format("Invalid class %s specified as table for @ForeignKey, "
                                    + "the specified class must be annotated with %s",
                            tableElem.toString(), SQLiteTable.class.getCanonicalName()));
        }

        Element fieldRefElement = null;
        for (final Element enc : tableElem.getEnclosedElements()) {
            if (!enc.getSimpleName().toString().equals(foreignKey.fieldReference())) {
                continue;
            }

            final SQLiteField field = enc.getAnnotation(SQLiteField.class);
            if (field == null || (!field.primaryKey().enabled() && !field.unique())) {
                throw new ProcessingException(enclosed,
                        String.format("Field %s in class %s needs to be declared as primary key or "
                                + "unique with @SQLiteField to be referenced in "
                                + "@ForeignKey", enc.toString(), tableElem.toString()));
            }

            fieldRefElement = enc;
            break;
        }

        if (fieldRefElement == null) {
            throw new ProcessingException(enclosed,
                    String.format("Field %s in class %s does not exist",
                            foreignKey.fieldReference(), tableElem.toString()));
        }

        return fieldRefElement;
    }

    private SQLiteTable getForeignKeyReferencedTable(final ForeignKey foreignKey) {
        TypeMirror mirror = null;
        try {
            foreignKey.table();
        } catch (MirroredTypeException ex) {
            mirror = ex.getTypeMirror();
        }

        return mTypeUtils.asElement(mirror).getAnnotation(SQLiteTable.class);
    }

    private String getIndent(final int n) {
        final StringBuilder builder = new StringBuilder("  ");

        for (int i = 0; i < n; i++) {
            builder.append("  ");
        }

        return builder.toString();
    }

    private String getCreateStatement(final Element element, final SQLiteTable table) {
        final StringBuilder createStatement = new StringBuilder("\n")
                .append(getIndent(1))
                .append("CREATE TABLE IF NOT EXISTS ")
                .append(table.tableName())
                .append(" (\n")
                .append(getIndent(2)),
                foreignKeys = new StringBuilder();

        final String endOfColumnSpec = ",\n" + getIndent(2);

        for (final Element enclosed : element.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            final String fieldName = getDBFieldName(enclosed, field);
            createStatement.append("`");
            createStatement.append(fieldName);
            createStatement.append("`");
            createStatement.append(" ");
            createStatement.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = field.primaryKey();
            if (primaryKey.enabled()) {
                createStatement.append(" PRIMARY KEY");
                createStatement.append(primaryKey.autoIncrement()
                        ? " AUTOINCREMENT" : "");
            }

            if (field.unique()) {
                createStatement.append(" UNIQUE");
            }

            if (field.notNull()) {
                createStatement.append(" NOT NULL");
            }

            final ForeignKey foreignKey = field.foreignKey();
            if (foreignKey.enabled()) {
                final Element foreignKeyRefElement = findForeignKeyReferencedField(enclosed,
                        foreignKey);
                final SQLiteTable foreignKeyRefTable = getForeignKeyReferencedTable(foreignKey);
                foreignKeys.append(String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                        fieldName, foreignKeyRefTable.tableName(),
                        getDBFieldName(foreignKeyRefElement,
                                foreignKeyRefElement.getAnnotation(SQLiteField.class))));
                if (foreignKey.cascadeOnDelete()) {
                    foreignKeys.append(" ON DELETE CASCADE");
                }

                if (foreignKey.cascadeOnUpdate()) {
                    foreignKeys.append(" ON UPDATE CASCADE");
                }

                foreignKeys.append(endOfColumnSpec);
            }

            createStatement.append(endOfColumnSpec);
        }

        final StringBuilder removeLastComma = new StringBuilder(createStatement.substring(0,
                createStatement.length() - (foreignKeys.length() > 0
                        ? endOfColumnSpec.length() - 2
                        : endOfColumnSpec.length())))
                .append(foreignKeys.length() > 0
                        ? getIndent(2) + foreignKeys.substring(0,
                        foreignKeys.length() - endOfColumnSpec.length())
                        : "")
                .append("\n")
                .append(getIndent(1))
                .append(");");

        return removeLastComma.toString();
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

    private FieldSpec buildInstanceField() {
        return FieldSpec.builder(getHelperClassName(), "sInstance", Modifier.PRIVATE,
                Modifier.STATIC)
                .build();
    }

    private MethodSpec buildCtor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("super(context, $L, null, $L)", "DATABASE_NAME", "DATABASE_VERSION")
                .build();
    }

    private MethodSpec buildGetInstanceMethod() {
        return MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SYNCHRONIZED)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .beginControlFlow("if (sInstance == null)")
                .addStatement("sInstance = new $T(context.getApplicationContext())",
                        getHelperClassName())
                .endControlFlow()
                .addStatement("return sInstance")
                .returns(getHelperClassName())
                .build();
    }

    private MethodSpec buildOnOpenMethod() {
        final CodeBlock.Builder code = CodeBlock.builder(),
                onOpenStatement = CodeBlock.builder();
        boolean foundForeignKey = false;

        for (final Element enclosed : mElement.getEnclosedElements()) {
            int onOpenCounter = 0;

            if (enclosed.getAnnotation(OnOpen.class) == null) continue;

            if (++onOpenCounter > 1) {
                throw new ProcessingException(enclosed,
                        String.format("Only one method per class may be annotated with %s",
                                OnOpen.class.getCanonicalName()));
            }

            if (!enclosed.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException(enclosed,
                        String.format("%s annotated methods need to be static",
                                OnOpen.class.getCanonicalName()));
            }

            final ExecutableElement executableElement = (ExecutableElement) enclosed;
            if (executableElement.getParameters().size() != 1
                    || !SQLITE_DATABASE.equals(
                    ClassName.get(executableElement.getParameters().get(0).asType()))) {
                throw new ProcessingException(enclosed,
                        String.format("%s annotated methods needs to have exactly "
                                        + "one parameter, being of type %s",
                                OnOpen.class.getCanonicalName(),
                                SQLITE_DATABASE.toString()));
            }

            onOpenStatement.addStatement("$T.$L(database)", mElement, enclosed.getSimpleName());
        }

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry
                : mTableElementMap.entrySet()) {

            for (final Element enclosed : tableElementEntry.getValue().getEnclosedElements()) {
                final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
                if (!foundForeignKey && field != null && field.foreignKey().enabled()) {

                    code.beginControlFlow("if (!database.isReadOnly())")
                            .addStatement("database.execSQL($S)", "PRAGMA foreign_keys=ON;")
                            .endControlFlow();
                    foundForeignKey = true;
                    break;
                }
            }
        }

        code.add(onOpenStatement.build());

        return MethodSpec.methodBuilder("onOpen")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addStatement("super.onOpen(database)")
                .addCode(code.build())
                .build();
    }

    private MethodSpec buildOnCreateMethod() {
        final CodeBlock.Builder code = CodeBlock.builder(),
                onCreateStatement = CodeBlock.builder();

        int onCreateCounter = 0;
        for (final Element enclosed : mElement.getEnclosedElements()) {
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

            onCreateStatement.addStatement("$T.$L(database)", ClassName.get(mElement.asType()),
                    enclosed.getSimpleName());
        }

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry
                : mTableElementMap.entrySet()) {

            final SQLiteTable table = tableElementEntry.getKey();
            final Element element = tableElementEntry.getValue();

            code.add(getCreateBlock(element, table));
        }

        code.add(onCreateStatement.build());

        return MethodSpec.methodBuilder("onCreate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                .addCode(code.build())
                .build();
    }

    private MethodSpec buildOnUpgradeMethod() {
        final CodeBlock.Builder code = CodeBlock.builder(),
                onUpgradeStatements = CodeBlock.builder();

        int onUpgradeCounter = 0;
        for (final Element enclosed : mElement.getEnclosedElements()) {
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
                    ClassName.get(mElement.asType()), enclosed.getSimpleName());
        }

        for (final Map.Entry<SQLiteTable, Element> tableElementEntry
                : mTableElementMap.entrySet()) {


            final SQLiteTable table = tableElementEntry.getKey();
            final Element element = tableElementEntry.getValue();

            code.addStatement("/****** BEGIN $L ******/", table.tableName());
            code.add(getUpgradeBlock(element, table));
            code.nextControlFlow("else");
            code.add("// $L did not exist, let's create it\n", table.tableName());
            code.add(getCreateBlock(element, table));
            code.endControlFlow();
            code.addStatement("/******  END $L  ******/", table.tableName());
        }

        code.add(onUpgradeStatements.build());

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
                className + "_OpenHelper")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(SQLITE_OPEN_HELPER)
                .addFields(Arrays.asList(
                        buildColNameIndexField(),
                        buildDbNameField(),
                        buildDbVersionField(),
                        buildInstanceField()
                ))
                .addMethods(Arrays.asList(
                        buildCtor(),
                        buildGetInstanceMethod(),
                        buildOnOpenMethod(),
                        buildOnCreateMethod(),
                        buildOnUpgradeMethod()
                ))
                .build();

        return JavaFile.builder(mPackageName, typeSpec)
                .addFileComment("Generated code from HighLite. Do not modify!")
                .build();
    }
}