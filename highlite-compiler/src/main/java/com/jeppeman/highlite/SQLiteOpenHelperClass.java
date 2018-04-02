package com.jeppeman.highlite;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final Element mElement;
    private final String mPackageName;
    private final String mDatabaseName;
    private final Map<Element, SQLiteTable> mTableElementMap;
    private final Elements mElementUtils;
    private final int mVersion;

    SQLiteOpenHelperClass(final Element element,
                          final String packageName,
                          final String databaseName,
                          final Map<Element, SQLiteTable> tableElementMap,
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
                ? CodeBlock.of("database.execSQL($S);\n", getCreateStatement(element))
                : CodeBlock.of("");
    }

    private CodeBlock getInitialRecreationBlock(final String tableName,
                                                final Map<String, String> columnsMap,
                                                final Map<String, String> foreignKeysMap) {

        final String cursorVarName = tableName + "Cursor",
                dbColsVarName = tableName + "Cols",
                colsToSaveVarName = tableName + "ColsToSave",
                createSqlCursorVarName = tableName + "CreateCursor",
                createSqlStatementVarName = tableName + "Create",
                foreignKeysSplitVarName = tableName + "ForeignKeysSplit",
                foreignKeysVarName = tableName + "ForeignKeys",
                shouldRecreateVarName = tableName + "ShouldRecreate",
                tableExistsVarName = tableName + "Exists",
                newUniqueColsVarName = tableName + "NewUniqueCols";

        final String currentFieldsVar = tableName + "CurrentFields";
        final CodeBlock.Builder currentFieldsPopulator = CodeBlock.builder();
        for (final Map.Entry<String, String> entry : columnsMap.entrySet()) {
            currentFieldsPopulator.addStatement("$L.put($S, new $T[] { \n$S, \n$S })",
                    currentFieldsVar, entry.getKey(), STRING, entry.getValue(),
                    foreignKeysMap.containsKey(entry.getKey())
                            ? foreignKeysMap.get(entry.getKey())
                            : "");
        }

        return CodeBlock.builder()
                .add("// Check whether $L exists or not\n", tableName)
                .addStatement("boolean $L", tableExistsVarName)
                .addStatement("final $T $L = database.rawQuery(\n\"SELECT `sql` FROM "
                                + "sqlite_master WHERE `type` = ? AND `name` = ?;\", \n"
                                + "new $T[] { $S, $S })",
                        CURSOR, createSqlCursorVarName, STRING, "table", tableName)
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
                        tableExistsVarName, tableName)
                .add("// Collect columns based on current state of the class\n")
                .addStatement("final $T<String, String[]> $L = new $T<>()", MAP,
                        currentFieldsVar, LINKED_HASHMAP)
                .add(currentFieldsPopulator.build())
                .add("\n")
                .add("// Fetch current columns from the database\n", tableName)
                .addStatement("final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", "
                        + "null)", CURSOR, cursorVarName, tableName)
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
                .add("\n// Remove closing parenthesis of create statement definitions\n")
                .addStatement("$L = $L.substring(0, $L.length() - 1) + $S",
                        createSqlStatementVarName, createSqlStatementVarName,
                        createSqlStatementVarName, ", ")
                .addStatement("$T $L = new $T()", STRING_BUILDER, colsToSaveVarName,
                        STRING_BUILDER)
                .add("\n// Split create statement into column definitions and foreign key "
                        + "definitions\n")
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
                .addStatement("final $T<$T> $L = new $T<>()", LIST, STRING, newUniqueColsVarName,
                        ARRAY_LIST)
                .build();
    }

    private CodeBlock getUpgradeTableCopyBlock(final Element element, final SQLiteTable table) {
        final String tableName = getTableName(element),
                colsToSaveVarName = tableName + "ColsToSave",
                createSqlStatementVarName = tableName + "Create",
                foreignKeysVarName = tableName + "ForeignKeys",
                shouldRecreateVarName = tableName + "ShouldRecreate",
                tableExistsVarName = tableName + "Exists",
                newUniqueColsVarName = tableName + "NewUniqueCols";

        return CodeBlock.builder()
                .add("\n// Remove last comma from new column definitions\n")
                .beginControlFlow("if ($L.length() > 0)", colsToSaveVarName)
                .addStatement("$L = new $T($L.substring(0, \n$L.length() - 2))",
                        colsToSaveVarName, STRING_BUILDER, colsToSaveVarName, colsToSaveVarName)
                .endControlFlow()
                .add("\n// Wrap up the new create statement by combining column definition and "
                        + "foreign key definition\n// parts, as well as removing the last comma"
                        + " and adding the last parenthesis\n")
                .addStatement("$L = ($L + $L).substring(0, \n($L + $L).length() - 2) + $S",
                        createSqlStatementVarName, createSqlStatementVarName,
                        foreignKeysVarName, createSqlStatementVarName,
                        foreignKeysVarName, ");")
                .add("\n// Create a new table and copy the data from the old table to it and "
                        + "rename it after\n")
                .beginControlFlow("if ($L && $L)", shouldRecreateVarName, tableExistsVarName)
                .add(getRecreateStatement("database", tableName,
                        createSqlStatementVarName, colsToSaveVarName))
                .endControlFlow()
                .add("\n// Add any new unique indices\n")
                .beginControlFlow("if ($L.size() > 0)", newUniqueColsVarName)
                .addStatement("database.execSQL($S)", "BEGIN TRANSACTION;")
                .beginControlFlow("for (final String field : $L)", newUniqueColsVarName)
                .addStatement("database.execSQL($T.format("
                                + "\"CREATE UNIQUE INDEX `%s` ON $L(`%s`);\", field, field))",
                        STRING, tableName)
                .endControlFlow()
                .addStatement("database.execSQL($S)", "COMMIT;")
                .endControlFlow()
                .add("\n")
                .build();
    }

    private CodeBlock getUpgradeBlock(final Element element, final SQLiteTable table) {
        final String tableName = getTableName(element),
                currentFieldsVar = tableName + "CurrentFields",
                dbColsVarName = tableName + "Cols",
                colsToSaveVarName = tableName + "ColsToSave",
                createSqlStatementVarName = tableName + "Create",
                foreignKeysVarName = tableName + "ForeignKeys",
                shouldRecreateVarName = tableName + "ShouldRecreate",
                tableExistsVarName = tableName + "Exists",
                newUniqueColsVarName = tableName + "NewUniqueCols";

        final CodeBlock.Builder recreateStatement = CodeBlock.builder();
        final Map<String, String> columnsMap = new LinkedHashMap<>(),
                foreignKeysMap = new LinkedHashMap<>();

        for (final Element enclosed : getFields(element)) {
            final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);

            if (field == null) continue;

            final StringBuilder fieldCreator = new StringBuilder();
            final String fieldName = getDBFieldName(enclosed, getTableName(element));
            fieldCreator.append("`");
            fieldCreator.append(fieldName);
            fieldCreator.append("`");
            fieldCreator.append(" ");
            fieldCreator.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = field.primaryKey();
            if (primaryKey.enabled()) {
                final String tableNameOfPrimary = findTableNameOfElement(element, enclosed);
                fieldCreator.append(" PRIMARY KEY");
                if (tableName.equals(tableNameOfPrimary)) {
                    fieldCreator.append(
                            primaryKey.autoIncrement()
                                    ? " AUTOINCREMENT"
                                    : ""
                    );
                } else {
                    fieldCreator.append(" NOT NULL");
                    String foreignKeyBuilder = String.format(
                            "FOREIGN KEY(`%s`) REFERENCES %s(`%s`)", fieldName,
                            tableNameOfPrimary,
                            getDBFieldName(enclosed, null))
                            + " ON DELETE CASCADE"
                            + " ON UPDATE CASCADE, ";

                    foreignKeysMap.put(fieldName, foreignKeyBuilder);
                }
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
                final StringBuilder foreignKeyBuilder = new StringBuilder();
                foreignKeyBuilder.append(
                        String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                                fieldName, findTableNameOfElement(
                                        mTypeUtils.asElement(enclosed.asType()),
                                        foreignKeyRefElement),
                                getDBFieldName(foreignKeyRefElement, null)));
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

        final CodeBlock initialRecreationBlock = getInitialRecreationBlock(tableName, columnsMap,
                foreignKeysMap);

        if (table.autoAddColumns() && table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .addStatement("$L = $S", foreignKeysVarName, "")
                    .addStatement("$L = $S", createSqlStatementVarName, "CREATE TABLE "
                            + tableName + " (")
                    .add("// Match database fields against class fields to see if anything needs to"
                            + " be added\n")
                    .beginControlFlow("for (final $T.Entry<$T, $T[]> entry : $L.entrySet())", MAP,
                            STRING, STRING, currentFieldsVar)
                    .beginControlFlow("if (!$L.contains(entry.getKey()))", dbColsVarName)
                    .addStatement("$L = true", shouldRecreateVarName)
                    .beginControlFlow("if (entry.getValue()[0].contains($S))", " UNIQUE")
                    .add("// Mark column for UNIQUE constraint addition after table has been "
                            + "recreated\n")
                    .addStatement("$L.add(entry.getValue()[0].split($S)[1])", newUniqueColsVarName,
                            "`")
                    .endControlFlow()
                    .add("// Adding the new column to the table so that the tables that will be "
                            + "copied between have matching columns\n")
                    .add("// Since UNIQUE columns can't be added, remove constraint if it exists. "
                            + "\n// It will be added when the table is recreated\n")
                    .addStatement("database.execSQL($S + entry.getValue()[0].replace($S, $S))",
                            "ALTER TABLE `" + tableName + "` ADD COLUMN ", " UNIQUE", "")
                    .endControlFlow()
                    .add("\n")
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(entry.getKey())", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .addStatement("$L += entry.getValue()[1]", foreignKeysVarName)
                    .addStatement("$L += entry.getValue()[0] + $S", createSqlStatementVarName, ", ")
                    .endControlFlow()
                    .add(getUpgradeTableCopyBlock(element, table));
        } else if (table.autoAddColumns() && !table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .add("// Match database fields against class fields to see if anything needs to"
                            + " be added / removed\n")
                    .beginControlFlow("for (final $T.Entry<$T, $T[]> entry : $L.entrySet())", MAP,
                            STRING, STRING, currentFieldsVar)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(entry.getKey())", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .beginControlFlow("if ($L.contains(entry.getKey()))", dbColsVarName)
                    .add("// Replace the column definition of the create statement with the new "
                            + "column definition\n")
                    .addStatement("$L = $L.replaceAll(\n\"`?\" + entry.getKey() + \"`?\\\\s[^,]+\","
                                    + " \nentry.getValue()[0])", createSqlStatementVarName,
                            createSqlStatementVarName)
                    .beginControlFlow("if (entry.getValue()[1].length() > 0)")
                    .add("// Replace the foreign key definition of the create statement with the "
                            + "new foreign key definition\n")
                    .addStatement("$T fk = entry.getValue()[1]", STRING)
                    .addStatement("$L = $L.replaceAll(\n\"FOREIGN KEY\\\\(`?\" + entry.getKey() + "
                                    + "\"`?\\\\)[^,]+\", \nfk.substring(0, fk.length() - 2))",
                            foreignKeysVarName, foreignKeysVarName)
                    .endControlFlow()
                    .beginControlFlow("if (entry.getValue()[0].contains($S))", " UNIQUE")
                    .add("// Mark column for UNIQUE constraint addition after table has been "
                            + "recreated\n")
                    .addStatement("$L.add(entry.getValue()[0].split($S)[1])", newUniqueColsVarName,
                            "`")
                    .endControlFlow()
                    .addStatement("$L = true", shouldRecreateVarName)
                    .addStatement("continue")
                    .endControlFlow()
                    .add("\n")
                    .addStatement("$L = true", shouldRecreateVarName)
                    .add("// Adding the new column to the table so that the tables that will be "
                            + "copied between have matching columns\n")
                    .add("// Since UNIQUE columns can't be added, remove constraint if it exists. "
                            + "\n// It will be added when the table is recreated\n")
                    .addStatement("database.execSQL($S \n+ entry.getValue()[0].replace($S, $S))",
                            "ALTER TABLE `" + tableName + "` ADD COLUMN ", " UNIQUE", "")
                    .add("// Add new column and foreign key definitions to create statement\n")
                    .addStatement("$L += entry.getValue()[1]", foreignKeysVarName)
                    .addStatement("$L += entry.getValue()[0] + $S", createSqlStatementVarName, ", ")
                    .endControlFlow()
                    .add("\n// Saving any columns that were found in the database but are not in "
                            + "the current class definition\n")
                    .beginControlFlow("for (final $T column : $L)", STRING, dbColsVarName)
                    .beginControlFlow("if (!$L.containsKey(column))", currentFieldsVar)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append(column)", colsToSaveVarName)
                    .addStatement("$L.append($S)", colsToSaveVarName, "`")
                    .addStatement("$L.append($S)", colsToSaveVarName, ", ")
                    .endControlFlow()
                    .endControlFlow()
                    .add(getUpgradeTableCopyBlock(element, table));
        } else if (!table.autoAddColumns() && table.autoDeleteColumns()) {
            recreateStatement
                    .add(initialRecreationBlock)
                    .addStatement("$L = $S", foreignKeysVarName, "")
                    .addStatement("$L = $S", createSqlStatementVarName, "CREATE TABLE "
                            + tableName + " (")
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
                    .add(getRecreateStatement("database", tableName,
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

    private String getIndent(final int n) {
        final StringBuilder builder = new StringBuilder("  ");

        for (int i = 0; i < n; i++) {
            builder.append("  ");
        }

        return builder.toString();
    }

    private String getCreateStatement(final Element element) {
        final StringBuilder createStatement = new StringBuilder("\n")
                .append(getIndent(1))
                .append("CREATE TABLE IF NOT EXISTS ")
                .append(getTableName(element))
                .append(" (\n")
                .append(getIndent(2)),
                foreignKeys = new StringBuilder();

        final String endOfColumnSpec = ",\n" + getIndent(2);

        for (final Element enclosed : getFields(element)) {
            final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
            if (field == null) continue;

            final String tableName = getTableName(element);
            final String fieldName = getDBFieldName(enclosed, tableName);
            createStatement.append("`");
            createStatement.append(fieldName);
            createStatement.append("`");
            createStatement.append(" ");
            createStatement.append(getFieldType(enclosed, field));

            final PrimaryKey primaryKey = field.primaryKey();
            if (primaryKey.enabled()) {
                final String tableNameOfPrimaryKey = findTableNameOfElement(element, enclosed);
                createStatement.append(" PRIMARY KEY");
                if (tableName.equals(tableNameOfPrimaryKey)) {
                    createStatement.append(
                            primaryKey.autoIncrement()
                                    ? " AUTOINCREMENT"
                                    : ""
                    );
                } else {
                    createStatement.append(" NOT NULL");
                    foreignKeys.append(String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                            fieldName, tableNameOfPrimaryKey,
                            getDBFieldName(enclosed, null)));
                    foreignKeys.append(" ON DELETE CASCADE");
                    foreignKeys.append(" ON UPDATE CASCADE");
                    foreignKeys.append(endOfColumnSpec);
                }
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
                foreignKeys.append(String.format("FOREIGN KEY(`%s`) REFERENCES %s(`%s`)",
                        fieldName, findTableNameOfElement(mTypeUtils.asElement(enclosed.asType()),
                                foreignKeyRefElement),
                        getDBFieldName(foreignKeyRefElement, null)));
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

        for (final Map.Entry<Element, SQLiteTable> tableElementEntry
                : mTableElementMap.entrySet()) {

            for (final Element enclosed : getFields(tableElementEntry.getKey())) {
                final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
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

        for (final Map.Entry<Element, SQLiteTable> tableElementEntry
                : mTableElementMap.entrySet()) {

            final SQLiteTable table = tableElementEntry.getValue();
            final Element element = tableElementEntry.getKey();

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

    private List<MethodSpec> buildOnUpgradeSubMethods() {
        final List<MethodSpec> ret = new ArrayList<>();

        for (final Map.Entry<Element, SQLiteTable> tableElementEntry
                : mTableElementMap.entrySet()) {

            final CodeBlock.Builder code = CodeBlock.builder();
            final SQLiteTable table = tableElementEntry.getValue();
            final Element element = tableElementEntry.getKey();
            final String tableName = getTableName(element);

            code.add(getUpgradeBlock(element, table));
            if (table.autoCreate()) {
                code.nextControlFlow("else");
                code.add("// $L did not exist, let's create it\n", tableName);
                code.add(getCreateBlock(element, table));
            }
            code.endControlFlow();

            ret.add(MethodSpec.methodBuilder(String.format("onUpgrade%s", element.getSimpleName()))
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(SQLITE_DATABASE, "database", Modifier.FINAL)
                    .addCode(code.build())
                    .build());
        }

        return ret;
    }

    private MethodSpec buildOnUpgradeMethod() {
        final CodeBlock.Builder onUpgradeStatements = CodeBlock.builder(),
                code = CodeBlock.builder();

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

        for (final Map.Entry<Element, SQLiteTable> tableElementEntry
                : mTableElementMap.entrySet()) {

            code.addStatement("onUpgrade$L(database)", tableElementEntry.getKey().getSimpleName());
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
                .addMethods(buildOnUpgradeSubMethods())
                .build();

        return JavaFile.builder(mPackageName, typeSpec)
                .addFileComment("Generated code from HighLite. Do not modify!")
                .build();
    }
}