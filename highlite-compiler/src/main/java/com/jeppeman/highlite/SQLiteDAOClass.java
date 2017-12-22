package com.jeppeman.highlite;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Generator of data access objects to automatically handle insertion/updates/deletion of records
 * in a table described in classes annotated with {@link SQLiteTable}. The methods of these
 * generated classes are called from the corresponding methods of {@link SQLiteObject}s.
 *
 * @author jeppeman
 */
final class SQLiteDAOClass extends JavaWritableClass {

    private static final String INSTANCE_CACHE_VAR_NAME = "INSTANCE_CACHE";
    private static final String COLUMN_FIELD_MAP_VAR_NAME = "COLUMN_FIELD_MAP";

    private final String mHelperPackage;
    private final String mDatabaseName;
    private final SQLiteTable mTable;
    private final Element mElement;
    private final Elements mElementUtils;

    SQLiteDAOClass(final String helperPackage,
                   final String databaseName,
                   final SQLiteTable table,
                   final Element element,
                   final Elements elementUtils,
                   final Types typeUtils) {
        mHelperPackage = helperPackage;
        mDatabaseName = databaseName;
        mTable = table;
        mElement = element;
        mElementUtils = elementUtils;
        mTypeUtils = typeUtils;
    }

    private TypeMirror getTypeMirror(Class<?> cls) {
        return mElementUtils.getTypeElement(cls.getName()).asType();
    }

    private String getPackageName() {
        return mElementUtils
                .getPackageOf(mElement)
                .getQualifiedName()
                .toString();
    }

    private ClassName getClassNameOfElement() {
        return ClassName.get((TypeElement) mElement);
    }

    private ClassName getHelperClassName() {
        return ClassName.get(mHelperPackage,
                String.valueOf(mDatabaseName.charAt(0)).toUpperCase()
                        + mDatabaseName.substring(1) + "_OpenHelper");
    }

    private Element getPrimaryKeyField(final Element enclosing) {
        for (final Element enclosed : getFields(enclosing)) {
            final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
            if (field == null) continue;

            final PrimaryKey pk = field.primaryKey();
            if (pk.enabled()) return enclosed;
        }

        throw new ProcessingException(mElement, String.format("No primary key field defined for %s",
                mElement.getSimpleName()));
    }

    private Element getPrimaryKeyField() {
        return getPrimaryKeyField(mElement);
    }

    private List<MethodSpec> buildGetContentValuesMethods() {
        final List<MethodSpec> ret = new ArrayList<>();
        final String contentValsVar = "contentValues";

        CodeBlock.Builder putStatements = CodeBlock.builder();
        for (final Map.Entry<Element, List<Element>> typeFieldsEntry
                : getTypeFieldMap(mElement).entrySet()) {
            for (final Element enclosed : typeFieldsEntry.getValue()) {
                final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
                if (field == null) continue;

                final PrimaryKey pk = field.primaryKey();
                if (pk.enabled() && pk.autoIncrement()) {
                    continue;
                }

                final ForeignKey fk = field.foreignKey();

                final String fieldType = getFieldType(enclosed, field),
                        fieldName = "`" + getDBFieldName(enclosed,
                                enclosed.equals(typeFieldsEntry.getKey())
                                        ? getTableName(mElement)
                                        : null) + "`";

                final CodeBlock.Builder putStatement = CodeBlock.builder();
                if (fk.enabled()) {
                    final Element foreignKeyRefElement = findForeignKeyReferencedField(enclosed,
                            fk);
                    final TypeName foreignKeyRefElementTypeName = ClassName.get(
                            foreignKeyRefElement.asType());
                    if (foreignKeyRefElementTypeName.equals(TypeName.SHORT)
                            || foreignKeyRefElementTypeName.equals(TypeName.INT)
                            || foreignKeyRefElementTypeName.equals(TypeName.LONG)) {
                        putStatement.beginControlFlow("if (mTarget.$L != null)", enclosed
                                .getSimpleName());
                        putStatement.addStatement("$L.put($S, mTarget.$L.$L)", contentValsVar,
                                fieldName, enclosed.getSimpleName(),
                                foreignKeyRefElement.getSimpleName());
                        putStatement.endControlFlow();
                    } else {
                        putStatement.beginControlFlow("if (mTarget.$L != null",
                                enclosed.getSimpleName());
                        putStatement.addStatement("$L.put($S, mTarget.$L.$L)", contentValsVar,
                                fieldName, enclosed.getSimpleName(),
                                foreignKeyRefElement.getSimpleName());
                        putStatement.endControlFlow();
                    }
                } else if (SQLiteColumnType.valueOf(fieldType) == SQLiteColumnType.BLOB) {
                    putStatement.beginControlFlow("try")
                            .addStatement("final $T baos = new $T()", BYTE_ARRAY_OS, BYTE_ARRAY_OS)
                            .addStatement("final $T oos = new $T(baos)", OBJECT_OS, OBJECT_OS)
                            .addStatement("oos.writeObject(mTarget.$L)", enclosed.getSimpleName())
                            .addStatement("$L.put($S, baos.toByteArray())", contentValsVar,
                                    fieldName)
                            .nextControlFlow("catch ($T e)", IO_EXCEPTION)
                            .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                            .endControlFlow();
                } else if (DATE.equals(ClassName.get(enclosed.asType()))) {
                    putStatement.beginControlFlow("if (mTarget.$L != null)",
                            enclosed.getSimpleName());
                    putStatement.addStatement("$L.put($S, mTarget.$L.getTime())", contentValsVar,
                            fieldName, enclosed.getSimpleName());
                    putStatement.endControlFlow();
                } else {
                    putStatement.addStatement("$L.put($S, mTarget.$L)", contentValsVar, fieldName,
                            enclosed.getSimpleName());
                }

                putStatements.add(putStatement.build());
            }

            ret.add(MethodSpec.methodBuilder(String.format("getContentValues%s",
                    typeFieldsEntry.getKey().getSimpleName()))
                    .addModifiers(Modifier.PRIVATE)
                    .returns(CONTENT_VALUES)
                    .addStatement("final $T $L = new $T()", CONTENT_VALUES, contentValsVar,
                            CONTENT_VALUES)
                    .addCode(putStatements.build())
                    .addStatement("return $L", contentValsVar)
                    .build());

            putStatements = CodeBlock.builder();
        }

        return ret;
    }

    private FieldSpec buildTargetField() {
        return FieldSpec.builder(getClassNameOfElement(), "mTarget", Modifier.PRIVATE,
                Modifier.FINAL)
                .build();
    }

    private FieldSpec buildInstanceCacheField() {
        TypeName pkTypeName = ClassName.get(getPrimaryKeyField().asType());
        if (pkTypeName == ClassName.SHORT) {
            pkTypeName = ClassName.get(Short.class);
        } else if (pkTypeName == ClassName.INT) {
            pkTypeName = ClassName.get(Integer.class);
        } else if (pkTypeName == ClassName.LONG) {
            pkTypeName = ClassName.get(Long.class);
        }

        return FieldSpec.builder(ParameterizedTypeName.get(CONCURRENT_MAP, pkTypeName,
                ClassName.get(mElement.asType())), INSTANCE_CACHE_VAR_NAME,
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", CONCURRENT_HASHMAP)
                .build();
    }

    private FieldSpec buildFieldColumnMapField() {
        return FieldSpec.builder(ParameterizedTypeName.get(MAP, STRING, STRING),
                COLUMN_FIELD_MAP_VAR_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", HASHMAP)
                .build();
    }

    private CodeBlock getStaticInitializer() {
        final CodeBlock.Builder putStatements = CodeBlock.builder();
        boolean primaryKeyAdded = false;
        for (final Map.Entry<Element, List<Element>> entry : getTypeFieldMap(mElement).entrySet()) {
            final List<Element> allElements = getFields(entry.getKey());
            for (int i = 0; i < allElements.size(); i++) {
                final Element enclosed = allElements.get(i);
                final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
                if (field == null) continue;

                if (field.primaryKey().enabled() && primaryKeyAdded) continue;
                else if (field.primaryKey().enabled()) primaryKeyAdded = true;

                final String columnName = getDBFieldName(enclosed, getTableName(entry.getKey()));
                putStatements.addStatement("$L.put($S, $S)", COLUMN_FIELD_MAP_VAR_NAME, columnName,
                        enclosed.getSimpleName().toString());
            }
        }

        return putStatements.build();
    }

    private MethodSpec buildCtor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getClassNameOfElement(), "target", Modifier.FINAL)
                .addStatement("mTarget = target")
                .build();
    }

    private MethodSpec buildGetReadableDatabaseMethod() {
        return MethodSpec.methodBuilder(
                "getReadableDatabase")
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .returns(SQLITE_DATABASE)
                .addStatement("return $T.getInstance(context).getReadableDatabase()",
                        getHelperClassName())
                .build();
    }

    private MethodSpec buildGetWritableDatabaseMethod() {
        return MethodSpec.methodBuilder(
                "getWritableDatabase")
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .returns(SQLITE_DATABASE)
                .addStatement("return $T.getInstance(context).getWritableDatabase()",
                        getHelperClassName())
                .build();
    }

    private List<MethodSpec> buildSaveSubMethods() {
        final List<MethodSpec> ret = new ArrayList<>();
        for (final Map.Entry<Element, List<Element>> entry : getTypeFieldMap(mElement).entrySet()) {
            final Element primaryKeyElement = getPrimaryKeyField(entry.getKey());
            final String cursorVarName = "cursor";

            if (primaryKeyElement == null) {
                throw new ProcessingException(mElement,
                        String.format("%s must contain a field annotated with %s",
                                mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
            }

            final String pkFieldName = "`" + getDBFieldName(primaryKeyElement,
                    !primaryKeyElement.getEnclosingElement().equals(entry.getKey())
                            ? getTableName(mElement)
                            : null) + "`";

            ret.add(MethodSpec.methodBuilder(
                    String.format("save%s", entry.getKey().getSimpleName()))
                    .returns(TypeName.INT)
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(CONTEXT, "context", Modifier.FINAL)
                    .addStatement("final $T $L = getReadableDatabase($L)"
                                    + ".rawQuery(\n$S, \nnew $T[] { $T.valueOf(mTarget.$L) })",
                            CURSOR, cursorVarName, "context",
                            String.format("SELECT COUNT(*) FROM %s WHERE %s = ?",
                                    getTableName(entry.getKey()), pkFieldName), STRING, STRING,
                            primaryKeyElement.getSimpleName())
                    .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                    .addStatement("$L.close()", cursorVarName)
                    .addCode(buildInsertBlock(entry.getKey()))
                    .nextControlFlow("else")
                    .addStatement("$T rowCount = $L.getInt(0)", TypeName.INT, cursorVarName)
                    .addStatement("$L.close()", cursorVarName)
                    .beginControlFlow("if (rowCount == 0)")
                    .addCode(buildInsertBlock(entry.getKey()))
                    .nextControlFlow("else")
                    .addCode(buildUpdateBlock(entry.getKey()))
                    .endControlFlow()
                    .endControlFlow()
                    .build());
        }

        return ret;
    }

    private MethodSpec buildSaveMethod() {
        final CodeBlock.Builder subSaveMethods = CodeBlock.builder();

        for (final Map.Entry<Element, List<Element>> entry : getTypeFieldMap(mElement).entrySet()) {
            subSaveMethods.addStatement("save$L(context)", entry.getKey().getSimpleName());
        }

        subSaveMethods.addStatement("return 1");

        return MethodSpec.methodBuilder("save")
                .addAnnotation(Override.class)
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("$L.clear()", INSTANCE_CACHE_VAR_NAME)
                .addCode(subSaveMethods.build())
                .build();
    }

    private CodeBlock buildInsertBlock(final Element enclosing) {
        final Element primaryKeyElement = getPrimaryKeyField(enclosing);

        if (primaryKeyElement == null) {
            throw new ProcessingException(enclosing,
                    String.format("%s must contain a field annotated with %s",
                            enclosing.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final CodeBlock.Builder setIdAfterInsertion = CodeBlock.builder();
        if (primaryKeyElement.getAnnotation(SQLiteColumn.class).primaryKey().autoIncrement()) {
            setIdAfterInsertion.addStatement("mTarget.$L = ($T)id",
                    primaryKeyElement.getSimpleName(), ClassName.get(primaryKeyElement.asType()));
        }

        return CodeBlock.builder()
                .add("final long id = ")
                .addStatement("getWritableDatabase($L)\n.insertOrThrow(\n$S, \nnull, "
                                + "\ngetContentValues$L())",
                        "context", getTableName(enclosing), enclosing.getSimpleName())
                .add(setIdAfterInsertion.build())
                .addStatement("return 1")
                .build();
    }

    private CodeBlock buildUpdateBlock(final Element enclosing) {
        final Element primaryKeyElement = getPrimaryKeyField(enclosing);

        if (primaryKeyElement == null) {
            throw new ProcessingException(enclosing,
                    String.format("%s must contain a field annotated with %s",
                            enclosing.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement, getTableName(enclosing))
                + "`";
        return CodeBlock.builder()
                .addStatement("return getWritableDatabase($L)"
                                + "\n.update(\n$S, \ngetContentValues$L(), \n$S, "
                                + "\nnew $T[] { $T.valueOf(mTarget.$L) })",
                        "context", getTableName(enclosing), enclosing.getSimpleName(),
                        pkFieldName + " = ?", STRING, STRING, primaryKeyElement.getSimpleName())
                .build();
    }

    private MethodSpec buildSaveByQueryMethod() {
        return MethodSpec.methodBuilder("saveByQuery")
                .returns(TypeName.INT)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(MAP, STRING, TypeName.OBJECT), "colsToSave",
                        Modifier.FINAL)
                .addParameter(STRING, "whereClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "whereArgs", Modifier.FINAL)
                .addStatement("$L.clear()", INSTANCE_CACHE_VAR_NAME)
                .addStatement("final $T values = new $T()", CONTENT_VALUES, CONTENT_VALUES)
                .beginControlFlow("for ($T entry : colsToSave.entrySet())",
                        ParameterizedTypeName.get(MAP_ENTRY, STRING, TypeName.OBJECT))
                .addStatement("final $T key = entry.getKey()", STRING)
                .addStatement("final $T value = entry.getValue()", TypeName.OBJECT)
                .beginControlFlow("if (value == null)")
                .addStatement("continue")
                .nextControlFlow("if (value instanceof $T)", DATE)
                .addStatement("values.put(key, (($T) value).getTime())", DATE)
                .nextControlFlow("else if (value instanceof Byte)")
                .addStatement("values.put(key, (Byte) value)")
                .nextControlFlow("else if (value instanceof Long)")
                .addStatement("values.put(key, (Long) value)")
                .nextControlFlow("else if (value instanceof Float)")
                .addStatement("values.put(key, (Float) value)")
                .nextControlFlow("else if (value instanceof Short)")
                .addStatement("values.put(key, (Short) value)")
                .nextControlFlow("else if (value instanceof byte[])")
                .addStatement("values.put(key, (byte[]) value)")
                .nextControlFlow("else if (value instanceof Double)")
                .addStatement("values.put(key, (Double) value)")
                .nextControlFlow("else if (value instanceof String)")
                .addStatement("values.put(key, (String) value)")
                .nextControlFlow("else if (value instanceof Boolean)")
                .addStatement("values.put(key, (Boolean) value)")
                .nextControlFlow("else if (value instanceof Integer)")
                .addStatement("values.put(key, (Integer) value)")
                .nextControlFlow("else")
                .beginControlFlow("try")
                .addStatement("final $T baos = new $T()", BYTE_ARRAY_OS, BYTE_ARRAY_OS)
                .addStatement("final $T oos = new $T(baos)", OBJECT_OS, OBJECT_OS)
                .addStatement("oos.writeObject(value)")
                .addStatement("values.put(key, baos.toByteArray())")
                .nextControlFlow("catch ($T e)", IO_EXCEPTION)
                .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("return getWritableDatabase($L)"
                                + ".update(\n$S, \nvalues, \nwhereClause, \nwhereArgs)",
                        "context", getTableName(mElement))
                .build();
    }

    private MethodSpec buildDeleteMethod() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement, getTableName(mElement))
                + "`";

        return MethodSpec.methodBuilder("delete")
                .returns(TypeName.INT)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("return getWritableDatabase($L)"
                                + ".delete(\n$S, \n$S, \nnew $T[] { $T.valueOf(mTarget.$L) })",
                        "context", getTableName(mElement), pkFieldName + " = ?", STRING, STRING,
                        primaryKeyElement.getSimpleName())
                .build();
    }

    private MethodSpec buildDeleteByQueryMethod() {
        return MethodSpec.methodBuilder("deleteByQuery")
                .returns(TypeName.INT)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "whereClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "whereArgs", Modifier.FINAL)
                .addStatement("return getWritableDatabase($L)"
                                + ".delete(\n$S, \nwhereClause, \nwhereArgs)",
                        "context", getTableName(mElement))
                .build();
    }

    private MethodSpec buildGetSingleByIdMethod() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement, getTableName(mElement))
                + "`";

        return MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getClassNameOfElement())
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(TypeName.OBJECT, "id", Modifier.FINAL)
                .addStatement("return getSingle(\n$L, \n$S, "
                                + "\nnew $T[] { $T.valueOf(id) }, \nnull, \nnull, \nnull, \nfalse)",
                        "context", pkFieldName + " = ?", STRING, STRING)
                .build();
    }

    private MethodSpec buildGetSingleByRawQueryMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getClassNameOfElement())
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "rawQueryClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "rawQueryArgs", Modifier.FINAL)
                .addParameter(TypeName.BOOLEAN, "fromCache", Modifier.FINAL)
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery(rawQueryClause, rawQueryArgs)",
                        CURSOR, cursorVarName, "context")
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T ret = instantiateObject(cursor, context, fromCache)",
                        getClassNameOfElement())
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private MethodSpec buildGetSingleMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getClassNameOfElement())
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "whereClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "whereArgs", Modifier.FINAL)
                .addParameter(STRING, "groupBy", Modifier.FINAL)
                .addParameter(STRING, "having", Modifier.FINAL)
                .addParameter(STRING, "orderBy", Modifier.FINAL)
                .addParameter(TypeName.BOOLEAN, "fromCache", Modifier.FINAL)
                .addStatement("final String sql = $S \n"
                                + "+ (whereClause != null ? $S + whereClause : $S)\n"
                                + "+ (groupBy != null ? $S + groupBy : $S)\n"
                                + "+ (having != null ? $S + groupBy : $S)\n"
                                + "+ (orderBy != null ? $S + orderBy : $S)\n"
                                + "+ $S",
                        getJoinStatementForFetch(), " WHERE ", "", " GROUP BY ", "", " HAVING ",
                        "", " ORDER BY ", "", " LIMIT 1 ")
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery(sql, whereArgs)",
                        CURSOR, cursorVarName, "context")
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T ret = instantiateObject(cursor, context, fromCache)",
                        getClassNameOfElement())
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private MethodSpec buildGetListByRawQueryMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getList")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, getClassNameOfElement()))
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "rawQueryClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "rawQueryArgs", Modifier.FINAL)
                .addParameter(TypeName.BOOLEAN, "fromCache", Modifier.FINAL)
                .addStatement("final $T<$T> ret = new $T<>()", LIST, getClassNameOfElement(),
                        ARRAY_LIST)
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery(rawQueryClause, rawQueryArgs)",
                        CURSOR, cursorVarName, "context")
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .endControlFlow()
                .beginControlFlow("do")
                .addStatement("ret.add(instantiateObject(cursor, context, fromCache))")
                .endControlFlow("while(cursor.moveToNext())")
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private String getJoinStatementForFetch() {
        final Element pkElem = getPrimaryKeyField();
        final String pkFieldName = getDBFieldName(pkElem, getTableName(mElement)),
                tableName = getTableName(mElement);
        final StringBuilder selectPart = new StringBuilder(
                String.format("SELECT %s.*", tableName)),
                joinPart = new StringBuilder();
        final List<Element> set = new ArrayList<>(getTypeFieldMap(mElement).keySet());

        for (int i = set.size() - 2; i >= 0; i--) {
            final Element element = set.get(i),
                    primaryKeyElem = getPrimaryKeyField(element);
            final String primaryKeyFieldName = getDBFieldName(primaryKeyElem,
                    getTableName(element)),
                    superTableName = getTableName(element);
            selectPart.append(String.format(", %s.*", superTableName));
            joinPart.append(String.format("INNER JOIN %s ON (%s.%s = %s.%s)\n",
                    superTableName, tableName, pkFieldName, superTableName, primaryKeyFieldName));
        }

        selectPart.append(String.format(" FROM %s ", tableName));

        if (set.size() > 1) {
            selectPart.append("\n");
        }

        return selectPart.toString() + joinPart.toString();
    }

    private MethodSpec buildGetListMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getList")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, getClassNameOfElement()))
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "whereClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "whereArgs", Modifier.FINAL)
                .addParameter(STRING, "groupBy", Modifier.FINAL)
                .addParameter(STRING, "having", Modifier.FINAL)
                .addParameter(STRING, "orderBy", Modifier.FINAL)
                .addParameter(STRING, "limit", Modifier.FINAL)
                .addParameter(TypeName.BOOLEAN, "fromCache", Modifier.FINAL)
                .addStatement("final String sql = $S \n"
                                + "+ (whereClause != null ? $S + whereClause : $S)\n"
                                + "+ (groupBy != null ? $S + groupBy : $S)\n"
                                + "+ (having != null ? $S + groupBy : $S)\n"
                                + "+ (orderBy != null ? $S + orderBy : $S)\n"
                                + "+ (limit != null ? $S + limit : $S)",
                        getJoinStatementForFetch(), " WHERE ", "", " GROUP BY ", "", " HAVING ",
                        "", " ORDER BY ", "", " LIMIT ", "")
                .addStatement("final $T<$T> ret = new $T<>()", LIST, getClassNameOfElement(),
                        ARRAY_LIST)
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery(sql, whereArgs)",
                        CURSOR, cursorVarName, "context")
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .endControlFlow()
                .beginControlFlow("do")
                .addStatement("ret.add(instantiateObject(cursor, context, fromCache))")
                .endControlFlow("while(cursor.moveToNext())")
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private Element findEnclosedRelationshipElement(final Element enclosing,
                                                    final String fieldName) {
        for (final Element enclosed : getFields(enclosing)) {
            final SQLiteRelationship rel = enclosed.getAnnotation(SQLiteRelationship.class);
            if (rel == null || !fieldName.equals(rel.backReference())) continue;

            return enclosed;
        }

        return null;
    }

    private Element findRelatedForeignKeyElement(final Element enclosing,
                                                 final String relatedFieldName) {
        for (final Element enclosed : getFields(enclosing)) {
            final SQLiteColumn sqliteColumn = enclosed.getAnnotation(SQLiteColumn.class);
            if (sqliteColumn == null
                    || !sqliteColumn.foreignKey().enabled()
                    || !relatedFieldName.equals(enclosed.getSimpleName().toString())) {
                continue;
            }

            return enclosed;
        }

        throw new ProcessingException(enclosing, String.format("Back reference %s not found, "
                + " referenced by %s", relatedFieldName, enclosing.getSimpleName()));
    }

    private String getCursorMethodFromTypeName(final TypeName typeName) {
        if (typeName.equals(TypeName.FLOAT)
                || typeName.equals(ClassName.get(Float.class))) {
            return "getFloat";
        } else if (typeName.equals(TypeName.DOUBLE)
                || typeName.equals(ClassName.get(Double.class))) {
            return "getDouble";
        } else if (typeName.equals(TypeName.SHORT)
                || typeName.equals(ClassName.get(Short.class))) {
            return "getShort";
        } else if (typeName.equals(TypeName.INT)
                || typeName.equals(ClassName.get(Integer.class))) {
            return "getInt";
        } else if (typeName.equals(TypeName.LONG)
                || typeName.equals(ClassName.get(Long.class))) {
            return "getLong";
        } else {
            return "getString";
        }
    }

    private MethodSpec buildInstantiateObjectMethod() {
        final ClassName elementCn = getClassNameOfElement();

        final CodeBlock.Builder sqliteFieldsBuilder = CodeBlock.builder(),
                relationshipsBuilder = CodeBlock.builder();
        boolean primaryKeyAdded = false;
        int relCounter = 1;
        for (final Map.Entry<Element, List<Element>> entry : getTypeFieldMap(mElement).entrySet()) {
            for (final Element enclosed : getFields(entry.getKey())) {
                final SQLiteColumn field = enclosed.getAnnotation(SQLiteColumn.class);
                if (field == null) {
                    final SQLiteRelationship relationship = enclosed
                            .getAnnotation(SQLiteRelationship.class);
                    if (relationship == null) continue;

                    final Element typeElement = mTypeUtils.asElement(enclosed.asType());
                    final Element relationClassElem;

                    if (mTypeUtils.isSameType(mTypeUtils.erasure(enclosed.asType()),
                            enclosed.asType())
                            && typeElement.getAnnotation(SQLiteTable.class) != null) {
                        relationClassElem = typeElement;
                    } else if (mTypeUtils.isAssignable(mTypeUtils.erasure(enclosed.asType()),
                            getTypeMirror(List.class))) {

                        List<? extends TypeMirror> typeArgs = ((DeclaredType) enclosed.asType())
                                .getTypeArguments();

                        if (typeArgs.size() == 0) {
                            throw new ProcessingException(enclosed, String.format("Missing "
                                            + "generic type parameter for field %s in class %s",
                                    enclosed.getSimpleName(), mElement.getSimpleName()));
                        }
                        relationClassElem = mTypeUtils.asElement(typeArgs.get(0));
                    } else {
                        throw new ProcessingException(enclosed, String.format("%s needs to be "
                                        + "assignable from %s or be annotated with %s",
                                enclosed.asType(), List.class.getCanonicalName(),
                                SQLiteTable.class.getCanonicalName()));
                    }

                    final Element relatedForeignElem = findRelatedForeignKeyElement(
                            relationClassElem, relationship.backReference());
                    final SQLiteColumn f = relatedForeignElem.getAnnotation(SQLiteColumn.class);

                    final TypeName tn = ClassName.get(
                            mElementUtils.getPackageOf(relatedForeignElem).toString(),
                            relationClassElem.getSimpleName().toString() + "_DAO");

                    final String dbFieldName = getDBFieldName(relatedForeignElem, null);

                    final CodeBlock.Builder relationshipBuilder = CodeBlock.builder()
                            .addStatement("final $T dao$L = new $T(null)", tn, relCounter, tn);

                    if (mTypeUtils.isSameType(mTypeUtils.erasure(enclosed.asType()),
                            enclosed.asType())) {
                        relationshipBuilder
                                .addStatement("ret.$L = dao$L.getSingle(\ncontext, \n\"`$L` = \" + "
                                                + "\n$T.valueOf(ret.$L), "
                                                + "\nnull, \nnull, \nnull, \nnull, "
                                                + "\ntrue)",
                                        enclosed.getSimpleName(), relCounter++, dbFieldName, STRING,
                                        f.foreignKey().fieldReference());
                    } else if (mTypeUtils.isAssignable(mTypeUtils.erasure(enclosed.asType()),
                            getTypeMirror(Collection.class))) {
                        relationshipBuilder
                                .addStatement("ret.$L = dao$L.getList(\ncontext, \n\"`$L` = \" + "
                                                + "\n$T.valueOf(ret.$L), "
                                                + "\nnull, \nnull, \nnull, \nnull, \nnull, "
                                                + "\ntrue)",
                                        enclosed.getSimpleName(), relCounter++, dbFieldName, STRING,
                                        f.foreignKey().fieldReference());
                    }


                    relationshipsBuilder.add(relationshipBuilder.build());

                    continue;
                }

                final Name fieldName = enclosed.getSimpleName();
                final TypeName typeName = ClassName.get(enclosed.asType());
                final ForeignKey foreignKey = field.foreignKey();
                final PrimaryKey pk = field.primaryKey();
                if (pk.enabled() && primaryKeyAdded) {
                    continue;
                }

                CodeBlock assignmentStatement;
                if (foreignKey.enabled()) {
                    final Element foreignKeyRefElement = findForeignKeyReferencedField(enclosed,
                            foreignKey);
                    final String dbFieldName = getDBFieldName(foreignKeyRefElement, null);
                    final TypeName foreignKeyRefElementTypeName = ClassName.get(
                            foreignKeyRefElement.asType());

                    final CodeBlock cursorBlock = CodeBlock.of("cursor.$L(i)",
                            getCursorMethodFromTypeName(foreignKeyRefElementTypeName));

                    final TypeName tn = ClassName.get(
                            mElementUtils.getPackageOf(foreignKeyRefElement).toString(),
                            findTableElementFromChild(
                                    mTypeUtils.asElement(enclosed.asType()),
                                    foreignKeyRefElement)
                                    .getSimpleName().toString() + "_DAO");

                    assignmentStatement = CodeBlock.builder()
                            .addStatement("final $T dao = new $T(null)", tn, tn)
                            .addStatement("ret.$L = dao.getSingle(context, $S, "
                                            + "new $T[] { $T.valueOf($L) }, true)",
                                    fieldName, String.format("SELECT * FROM %s WHERE"
                                                    + " `%s` = ? LIMIT 1",
                                            findTableNameOfElement(
                                                    mTypeUtils.asElement(enclosed.asType()),
                                                    foreignKeyRefElement),
                                            dbFieldName),
                                    STRING, STRING, cursorBlock.toString())
                            .build();
                } else if (typeName.equals(TypeName.BOOLEAN)
                        || typeName.equals(ClassName.get(Boolean.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getInt(i) != 0;\n",
                            fieldName);
                } else if (typeName.equals(TypeName.FLOAT)
                        || typeName.equals(ClassName.get(Float.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getFloat(i);\n",
                            fieldName);
                } else if (typeName.equals(TypeName.DOUBLE)
                        || typeName.equals(ClassName.get(Double.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getDouble(i);\n",
                            fieldName);
                } else if (typeName.equals(TypeName.SHORT)
                        || typeName.equals(ClassName.get(Short.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getShort(i);\n",
                            fieldName);
                } else if (typeName.equals(TypeName.INT)
                        || typeName.equals(ClassName.get(Integer.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getInt(i);\n",
                            fieldName);
                } else if (typeName.equals(TypeName.LONG)
                        || typeName.equals(ClassName.get(Long.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getLong(i);\n", fieldName);
                } else if (typeName.equals(TypeName.get(String.class))) {
                    assignmentStatement = CodeBlock.of("ret.$L = cursor.getString(i);\n",
                            fieldName);
                } else if (typeName.equals(DATE)) {
                    assignmentStatement = CodeBlock.of("ret.$L = new $T(cursor.getLong(i));\n",
                            fieldName, DATE);
                } else {
                    assignmentStatement = CodeBlock.builder()
                            .beginControlFlow("try")
                            .addStatement("final $T bis = new $T(cursor.getBlob(i))", BYTE_ARRAY_IS,
                                    BYTE_ARRAY_IS)
                            .addStatement("final $T ois = new $T(bis)", OBJECT_IS, OBJECT_IS)
                            .addStatement("ret.$L = ($T) ois.readObject()", fieldName,
                                    ClassName.get(enclosed.asType()))
                            .nextControlFlow("catch ($T | $T e)", IO_EXCEPTION,
                                    CLASS_NOT_FOUND_EXCEPTION)
                            .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                            .endControlFlow()
                            .build();
                }

                if (pk.enabled() && !primaryKeyAdded) {
                    primaryKeyAdded = true;
                    assignmentStatement = CodeBlock.builder()
                            .add(assignmentStatement)
                            .addStatement("$L.put(ret.$L, ret)", INSTANCE_CACHE_VAR_NAME,
                                    fieldName)
                            .addStatement("new $T().schedule(new $T() {\n"
                                            + "  @Override\n"
                                            + "  public void run() {\n"
                                            + "      $L.remove(ret.$L);\n"
                                            + "  }\n"
                                            + "}, 30 * 1000)", TIMER, TIMER_TASK,
                                    INSTANCE_CACHE_VAR_NAME, fieldName)
                            .build();
                }

                sqliteFieldsBuilder.beginControlFlow("if (fieldName.equals($S))",
                        enclosed.getSimpleName())
                        .add(assignmentStatement)
                        .addStatement("continue")
                        .endControlFlow();
            }
        }

        final Element pkElement = getPrimaryKeyField();
        final CodeBlock.Builder fetchFromCacheStatement = CodeBlock.builder();
        if (pkElement != null) {
            final SQLiteColumn field = pkElement.getAnnotation(SQLiteColumn.class);
            final PrimaryKey pk = field.primaryKey();
            if (pk.enabled()) {
                final TypeName pkTypeName = ClassName.get(pkElement.asType());
                fetchFromCacheStatement
                        .addStatement("final $T pkVal = cursor.$L(cursor.getColumnIndex($S))",
                                pkTypeName, getCursorMethodFromTypeName(pkTypeName),
                                getDBFieldName(pkElement, getTableName(mElement)))
                        .beginControlFlow("if (fromCache && $L.containsKey(pkVal))",
                                INSTANCE_CACHE_VAR_NAME)
                        .addStatement("return $L.get(pkVal)", INSTANCE_CACHE_VAR_NAME)
                        .endControlFlow();
            }
        }

        return MethodSpec.methodBuilder("instantiateObject")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CURSOR, "cursor", Modifier.FINAL)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(TypeName.BOOLEAN, "fromCache", Modifier.FINAL)
                .returns(elementCn)
                .addCode(fetchFromCacheStatement.build())
                .addStatement("final $T ret = new $T()", elementCn, elementCn)
                .beginControlFlow("for (int i = 0; i < cursor.getColumnCount(); i++)")
                .addStatement("final String name = cursor.getColumnName(i)")
                .beginControlFlow("if (!$L.containsKey(name))", COLUMN_FIELD_MAP_VAR_NAME)
                .addStatement("continue")
                .endControlFlow()
                .addStatement("final String fieldName = $L.get(name)", COLUMN_FIELD_MAP_VAR_NAME)
                .addCode(sqliteFieldsBuilder.build())
                .endControlFlow()
                .addCode(relationshipsBuilder.build())
                .addStatement("return ret")
                .build();
    }

    @Override
    JavaFile writeJava() {
        final TypeSpec typeSpec = TypeSpec.classBuilder(
                ClassName.bestGuess(getPackageName()
                        + "."
                        + getClassName((TypeElement) mElement, getPackageName()) + "_DAO"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(SQLITE_DAO, getClassNameOfElement()))
                .addStaticBlock(getStaticInitializer())
                .addFields(Arrays.asList(
                        buildInstanceCacheField(),
                        buildFieldColumnMapField(),
                        buildTargetField()
                ))
                .addMethods(buildGetContentValuesMethods())
                .addMethods(buildSaveSubMethods())
                .addMethods(Arrays.asList(
                        buildCtor(),
                        buildGetReadableDatabaseMethod(),
                        buildGetWritableDatabaseMethod(),
                        buildInstantiateObjectMethod(),
                        buildSaveMethod(),
                        buildSaveByQueryMethod(),
                        buildDeleteMethod(),
                        buildDeleteByQueryMethod(),
                        buildGetSingleByRawQueryMethod(),
                        buildGetSingleMethod(),
                        buildGetSingleByIdMethod(),
                        buildGetListByRawQueryMethod(),
                        buildGetListMethod()
                ))
                .build();

        return JavaFile.builder(getPackageName(), typeSpec)
                .addFileComment("Generated code from HighLite. Do not modify!")
                .build();
    }
}