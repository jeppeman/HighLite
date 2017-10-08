package com.jeppeman.liteomatic;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
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

    private final String mHelperPackage;
    private final String mDatabaseName;
    private final SQLiteTable mTable;
    private final Element mElement;
    private final Elements mElementUtils;
    private final Types mTypeUtils;

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

    private Element getPrimaryKeyField() {
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            final PrimaryKey pk = field.primaryKey();
            if (pk.enabled()) return enclosed;
        }

        return null;
    }

    private MethodSpec buildGetContentValuesMethod() {
        final String contentValsVar = "contentValues";

        final CodeBlock.Builder putStatements = CodeBlock.builder();
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null || field.isUpgradeAddColumnTest()) continue;

            final PrimaryKey pk = field.primaryKey();
            if (pk.enabled() && pk.autoIncrement()) {
                continue;
            }

            final String fieldType = getFieldType(enclosed, field),
                    fieldName = "`" + getDBFieldName(enclosed, field) + "`";

            final CodeBlock.Builder putStatement = CodeBlock.builder();
            if (SQLiteFieldType.valueOf(fieldType) == SQLiteFieldType.BLOB) {
                putStatement.beginControlFlow("try")
                        .addStatement("final $T baos = new $T()", BYTE_ARRAY_OS, BYTE_ARRAY_OS)
                        .addStatement("final $T oos = new $T(baos)", OBJECT_OS, OBJECT_OS)
                        .addStatement("oos.writeObject(mTarget.$L)", enclosed.getSimpleName())
                        .addStatement("$L.put($S, baos.toByteArray())", contentValsVar,
                                fieldName)
                        .nextControlFlow("catch ($T e)", IO_EXCEPTION)
                        .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                        .endControlFlow();
            } else {
                putStatement.addStatement("$L.put($S, mTarget.$L)", contentValsVar, fieldName,
                        enclosed.getSimpleName());
            }

            putStatements.add(putStatement.build());
        }

        return MethodSpec.methodBuilder(
                "getContentValues")
                .addModifiers(Modifier.PRIVATE)
                .returns(CONTENT_VALUES)
                .addStatement("final $T $L = new $T()", CONTENT_VALUES, contentValsVar,
                        CONTENT_VALUES)
                .addCode(putStatements.build())
                .addStatement("return $L", contentValsVar)
                .build();
    }

    private FieldSpec buildTargetField() {
        return FieldSpec.builder(getClassNameOfElement(), "mTarget", Modifier.PRIVATE,
                Modifier.FINAL)
                .build();
    }

    private FieldSpec buildColumnsField() {
        final CodeBlock.Builder arrayValues = CodeBlock.builder().add("new $T[] { ", STRING);
        for (int i = 0; i < mElement.getEnclosedElements().size(); i++) {
            final Element enclosed = mElement.getEnclosedElements().get(i);
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null
                    || field.isUpgradeAddColumnTest()
                    || field.isUpgradeDeleteColumnTest()) continue;

            arrayValues.add(i < mElement.getEnclosedElements().size() - 1
                    ? "$S, "
                    : "$S", getDBFieldName(enclosed, field));
        }

        arrayValues.add(" }");

        return FieldSpec.builder(ArrayTypeName.of(STRING), "COLUMNS",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(arrayValues.build())
                .build();
    }

    private FieldSpec buildFieldColumnMapField() {
        return FieldSpec.builder(ParameterizedTypeName.get(MAP, STRING, STRING), "COLUMN_FIELD_MAP",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", HASHMAP)
                .build();
    }

    private CodeBlock getStaticInitializer() {
        final CodeBlock.Builder putStatements = CodeBlock.builder();
        for (int i = 0; i < mElement.getEnclosedElements().size(); i++) {
            final Element enclosed = mElement.getEnclosedElements().get(i);
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null || field.isUpgradeAddColumnTest()) continue;

            final String columnName = field.value() == null || field.value().length() == 0
                    ? enclosed.getSimpleName().toString()
                    : field.value();
            putStatements.addStatement("$L.put($S, $S)", "COLUMN_FIELD_MAP", columnName,
                    enclosed.getSimpleName().toString());
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

    private MethodSpec buildSaveMethod() {
        final Element primaryKeyElement = getPrimaryKeyField();
        final String cursorVarName = "cursor";

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement,
                primaryKeyElement.getAnnotation(SQLiteField.class)) + "`";

        return MethodSpec.methodBuilder("save")
                .addAnnotation(Override.class)
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery($S, new $T[] { $T.valueOf(mTarget.$L) })",
                        CURSOR, cursorVarName, "context",
                        String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", mTable.tableName(),
                                pkFieldName), STRING, STRING, primaryKeyElement.getSimpleName())
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addCode(buildInsertBlock())
                .nextControlFlow("else")
                .addStatement("$T rowCount = $L.getInt(0)", TypeName.INT, cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .beginControlFlow("if (rowCount == 0)")
                .addCode(buildInsertBlock())
                .nextControlFlow("else")
                .addCode(buildUpdateBlock())
                .endControlFlow()
                .endControlFlow()
                .build();
    }

    private CodeBlock buildInsertBlock() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final CodeBlock.Builder setIdAfterInsertion = CodeBlock.builder();
        if (primaryKeyElement.getAnnotation(SQLiteField.class).primaryKey().autoIncrement()) {
            setIdAfterInsertion.addStatement("mTarget.$L = ($T)id",
                    primaryKeyElement.getSimpleName(), ClassName.get(primaryKeyElement.asType()));
        }

        return CodeBlock.builder()
                .add("final long id = ")
                .addStatement("getWritableDatabase($L).insertOrThrow($S, null, getContentValues())",
                        "context", mTable.tableName())
                .add(setIdAfterInsertion.build())
                .addStatement("return 1")
                .build();
    }

    private CodeBlock buildUpdateBlock() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement,
                primaryKeyElement.getAnnotation(SQLiteField.class)) + "`";
        return CodeBlock.builder()
                .addStatement("return getWritableDatabase($L)"
                                + ".update($S, getContentValues(), $S, "
                                + "new $T[] { $T.valueOf(mTarget.$L) })",
                        "context", mTable.tableName(), pkFieldName + " = ?", STRING, STRING,
                        primaryKeyElement.getSimpleName())
                .build();
    }

    private MethodSpec buildSaveByQueryMethod() {
        return MethodSpec.methodBuilder("saveByQuery")
                .returns(TypeName.INT)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "whereClause", Modifier.FINAL)
                .addParameter(ArrayTypeName.of(STRING), "whereArgs", Modifier.FINAL)
                .addStatement("return getWritableDatabase($L)"
                                + ".update($S, null, whereClause, whereArgs)",
                        "context", mTable.tableName())
                .build();
    }

    private MethodSpec buildDeleteMethod() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement,
                primaryKeyElement.getAnnotation(SQLiteField.class)) + "`";

        return MethodSpec.methodBuilder("delete")
                .returns(TypeName.INT)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("return getWritableDatabase($L)"
                                + ".delete($S, $S, new $T[] { $T.valueOf(mTarget.$L) })",
                        "context", mTable.tableName(), pkFieldName + " = ?", STRING, STRING,
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
                                + ".delete($S, whereClause, whereArgs)",
                        "context", mTable.tableName())
                .build();
    }

    private MethodSpec buildGetSingleByIdMethod() {
        final Element primaryKeyElement = getPrimaryKeyField();

        if (primaryKeyElement == null) {
            throw new ProcessingException(mElement,
                    String.format("%s must contain a field annotated with %s",
                            mElement.asType().toString(), PrimaryKey.class.getCanonicalName()));
        }

        final String pkFieldName = "`" + getDBFieldName(primaryKeyElement,
                primaryKeyElement.getAnnotation(SQLiteField.class)) + "`";

        return MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getClassNameOfElement())
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(TypeName.OBJECT, "id", Modifier.FINAL)
                .addStatement("return getSingle($L, $S, "
                                + "new $T[] { $T.valueOf(id) }, null, null, null)",
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
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".rawQuery(rawQueryClause, rawQueryArgs)",
                        CURSOR, cursorVarName, "context")
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T ret = instantiateObject(cursor, context)",
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
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".query($S, COLUMNS, whereClause, whereArgs, groupBy, having, "
                                + "orderBy, $S)",
                        CURSOR, cursorVarName, "context", mTable.tableName(), 1)
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T ret = instantiateObject(cursor, context)",
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
                .addStatement("ret.add(instantiateObject(cursor, context))")
                .endControlFlow("while(cursor.moveToNext())")
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
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
                .addStatement("final $T<$T> ret = new $T<>()", LIST, getClassNameOfElement(),
                        ARRAY_LIST)
                .addStatement("final $T $L = getReadableDatabase($L)"
                                + ".query($S, COLUMNS, whereClause, whereArgs, groupBy, having, "
                                + "orderBy, limit)",
                        CURSOR, cursorVarName, "context", mTable.tableName())
                .beginControlFlow("if (!$L.moveToFirst())", cursorVarName)
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .endControlFlow()
                .beginControlFlow("do")
                .addStatement("ret.add(instantiateObject(cursor, context))")
                .endControlFlow("while(cursor.moveToNext())")
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private MethodSpec buildInstantiateObjectMethod() {
        final ClassName elementCn = getClassNameOfElement();

        final CodeBlock.Builder sqliteFieldsBuilder = CodeBlock.builder(),
                relationshipsBuilder = CodeBlock.builder();
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null || field.isUpgradeAddColumnTest()) {
                final SQLiteRelationship relationship = enclosed
                        .getAnnotation(SQLiteRelationship.class);
                if (relationship == null) continue;

                TypeMirror mirror = null;
                try {
                    relationship.table();
                } catch (MirroredTypeException ex) {
                    mirror = ex.getTypeMirror();
                }

                final TypeName relationClassName = ClassName.get(mTypeUtils.asElement(mirror)
                        .asType());

                final CodeBlock.Builder relationshipBuilder = CodeBlock.builder()
                        .addStatement("ret.$L = $T.from(context, $T.class)"
                                        + ".getList().executeBlocking()",
                                enclosed.getSimpleName(), SQLITE_OPERATOR, relationClassName);

                relationshipsBuilder.add(relationshipBuilder.build());

                continue;
            }

            final Name fieldName = enclosed.getSimpleName();
            final TypeName typeName = ClassName.get(enclosed.asType());
            final CodeBlock assignmentStatement;
            if (typeName.equals(TypeName.BOOLEAN)
                    || typeName.equals(ClassName.get(Boolean.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getInt(i) != 0;\n", fieldName);
            } else if (typeName.equals(TypeName.FLOAT)
                    || typeName.equals(ClassName.get(Float.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getFloat(i);\n", fieldName);
            } else if (typeName.equals(TypeName.DOUBLE)
                    || typeName.equals(ClassName.get(Double.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getDouble(i);\n", fieldName);
            } else if (typeName.equals(TypeName.SHORT)
                    || typeName.equals(ClassName.get(Short.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getShort(i);\n", fieldName);
            } else if (typeName.equals(TypeName.INT)
                    || typeName.equals(ClassName.get(Integer.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getInt(i);\n", fieldName);
            } else if (typeName.equals(TypeName.LONG)
                    || typeName.equals(ClassName.get(Long.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getLong(i);\n", fieldName);
            } else if (typeName.equals(TypeName.get(String.class))) {
                assignmentStatement = CodeBlock.of("ret.$L = cursor.getString(i);\n", fieldName);
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

            sqliteFieldsBuilder.beginControlFlow("if (fieldName.equals($S))",
                    enclosed.getSimpleName())
                    .add(assignmentStatement)
                    .addStatement("continue")
                    .endControlFlow();
        }

        return MethodSpec.methodBuilder("instantiateObject")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(CURSOR, "cursor", Modifier.FINAL)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .returns(elementCn)
                .addStatement("final $T ret = new $T()", elementCn, elementCn)
                .beginControlFlow("for (int i = 0; i < cursor.getColumnCount(); i++)")
                .addStatement("final String name = cursor.getColumnName(i)")
                .addStatement("final String fieldName = COLUMN_FIELD_MAP.get(name)")
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
                        buildColumnsField(),
                        buildFieldColumnMapField(),
                        buildTargetField()
                ))
                .addMethods(Arrays.asList(
                        buildCtor(),
                        buildGetContentValuesMethod(),
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
                .addFileComment("Generated code from LiteOmatic. Do not modify!")
                .build();
    }
}