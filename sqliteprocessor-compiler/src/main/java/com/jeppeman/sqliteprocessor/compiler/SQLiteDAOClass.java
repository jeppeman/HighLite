package com.jeppeman.sqliteprocessor.compiler;

import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteTable;
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
import javax.lang.model.util.Elements;

/**
 * Created by jesper on 2016-08-25.
 */
final class SQLiteDAOClass extends JavaWritableClass {

    private final String mDatabaseName;
    private final SQLiteTable mTable;
    private final Element mElement;
    private final Elements mElementUtils;

    SQLiteDAOClass(final String databaseName,
                   final SQLiteTable table,
                   final Element element,
                   final Elements elementUtils) {
        mDatabaseName = databaseName;
        mTable = table;
        mElement = element;
        mElementUtils = elementUtils;
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
        return ClassName.get(getPackageName(),
                String.valueOf(mDatabaseName.charAt(0)).toUpperCase()
                        + mDatabaseName.substring(1) + "Helper");
    }

    private MethodSpec buildGetContentValuesMethod() {
        final String contentValsVar = "contentValues";

        final CodeBlock.Builder putStatements = CodeBlock.builder();
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            final String fieldType = getFieldType(enclosed, field),
                    fieldName = getFieldName(enclosed, field);

            final CodeBlock.Builder putStatement = CodeBlock.builder();
            if (SQLiteFieldType.valueOf(fieldType) == SQLiteFieldType.BLOB) {
                putStatement.beginControlFlow("try")
                        .addStatement("final $T baos = new $T()", BYTE_ARRAY_OS, BYTE_ARRAY_OS)
                        .addStatement("final $T oos = new $T(baos)", OBJECT_OS, OBJECT_OS)
                        .addStatement("oos.writeObject(mTarget.$L)", enclosed.getSimpleName())
                        .addStatement("$L.put($S, baos.toByteArray())", contentValsVar,
                                fieldName)
                        .endControlFlow()
                        .beginControlFlow("catch ($T e)", IO_EXCEPTION)
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
            if (field == null) continue;

            arrayValues.add(i < mElement.getEnclosedElements().size() - 1
                    ? "$S, "
                    : "$S", getFieldName(enclosed, field));
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
            if (field == null) continue;

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
                .addStatement("return new $T(context).getReadableDatabase()",
                        getHelperClassName())
                .build();
    }

    private MethodSpec buildGetWritableDatabaseMethod() {
        return MethodSpec.methodBuilder(
                "getWritableDatabase")
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .returns(SQLITE_DATABASE)
                .addStatement("return new $T(context).getWritableDatabase()",
                        getHelperClassName())
                .build();
    }

    private MethodSpec buildInsertMethod() {
        return MethodSpec.methodBuilder("insert")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("getWritableDatabase($L).insert($S, null, getContentValues())",
                        "context", mTable.tableName())
                .build();
    }

    private MethodSpec buildUpdateMethod() {
        return MethodSpec.methodBuilder("update")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();
    }

    private MethodSpec buildDeleteMethod() {
        return MethodSpec.methodBuilder("delete")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();
    }

    private MethodSpec buildQueryMethod() {
        return MethodSpec.methodBuilder("query")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();
    }

    private MethodSpec buildGetSingleMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getClassNameOfElement())
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(TypeName.OBJECT, "id", Modifier.FINAL)
                .addStatement("final $T $L = getWritableDatabase($L)"
                                + ".query($S, COLUMNS, null, null, null, null, null)",
                        CURSOR, cursorVarName, "context", mTable.tableName())
                .addStatement("if (!$L.moveToFirst()) return null", cursorVarName)
                .addStatement("$T ret = instantiateObject(cursor)", getClassNameOfElement())
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private MethodSpec buildGetCustomMethod() {
        final String cursorVarName = "cursor";
        return MethodSpec.methodBuilder("getCustom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, getClassNameOfElement()))
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "customWhere", Modifier.FINAL)
                .addStatement("final $T<$T> ret = new $T<>()", LIST, getClassNameOfElement(),
                        ARRAY_LIST)
                .addStatement("final $T $L = getWritableDatabase($L)"
                                + ".query($S, COLUMNS, customWhere, null, null, null, null)",
                        CURSOR, cursorVarName, "context", mTable.tableName())
                .addStatement("if (!$L.moveToFirst()) return ret", cursorVarName)
                .beginControlFlow("do")
                .addStatement("ret.add(instantiateObject(cursor))")
                .endControlFlow("while(cursor.moveToNext())")
                .addStatement("$L.close()", cursorVarName)
                .addStatement("return ret")
                .build();
    }

    private MethodSpec buildInstantiateObjectMethod() {
        final ClassName elementCn = getClassNameOfElement();

        final CodeBlock.Builder builder = CodeBlock.builder();
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            final Name fieldName = enclosed.getSimpleName();
            final TypeName typeName = ClassName.get(enclosed.asType());
            final CodeBlock assignMentStateMent;
            if (typeName.equals(TypeName.BOOLEAN)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getInt(i) != 0;\n", fieldName);
            } else if (typeName.equals(TypeName.FLOAT)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getFloat(i);\n", fieldName);
            } else if (typeName.equals(TypeName.DOUBLE)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getDouble(i);\n", fieldName);
            } else if (typeName.equals(TypeName.SHORT)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getShort(i);\n", fieldName);
            } else if (typeName.equals(TypeName.INT)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getInt(i);\n", fieldName);
            } else if (typeName.equals(TypeName.LONG)) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getLong(i);\n", fieldName);
            } else if (typeName.equals(TypeName.get(String.class))) {
                assignMentStateMent = CodeBlock.of("mTarget.$L = cursor.getString(i);\n", fieldName);
            } else {
                assignMentStateMent = CodeBlock.builder()
                        .beginControlFlow("try")
                        .addStatement("final $T bis = new $T(cursor.getBlob(i))", BYTE_ARRAY_IS,
                                BYTE_ARRAY_IS)
                        .addStatement("final $T ois = new $T(bis)", OBJECT_IS, OBJECT_IS)
                        .addStatement("mTarget.$L = ($T)ois.readObject()", fieldName,
                                ClassName.get(enclosed.asType()))
                        .endControlFlow()
                        .beginControlFlow("catch ($T | $T e)", IO_EXCEPTION,
                                CLASS_NOT_FOUND_EXCEPTION)
                        .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                        .endControlFlow()
                        .build();
            }

            builder.beginControlFlow("if (fieldName.equals($S))", enclosed.getSimpleName())
                    .add(assignMentStateMent)
                    .addStatement("continue")
                    .endControlFlow();
        }

        return MethodSpec.methodBuilder("instantiateObject")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(CURSOR, "cursor", Modifier.FINAL)
                .returns(elementCn)
                .addStatement("final $T ret = new $T()", elementCn, elementCn)
                .beginControlFlow("for (int i = 0; i < cursor.getColumnCount(); i++)")
                .addStatement("final String name = cursor.getColumnName(i)")
                .addStatement("final String fieldName = COLUMN_FIELD_MAP.get(name)")
                .addCode(builder.build())
                .endControlFlow()
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
                        buildInsertMethod(),
                        buildUpdateMethod(),
                        buildDeleteMethod(),
                        buildQueryMethod(),
                        buildGetSingleMethod(),
                        buildGetCustomMethod()
                ))
                .build();

        return JavaFile.builder(getPackageName(), typeSpec)
                .addFileComment("Generated code from SQLiteProcessor. Do not modify!")
                .build();
    }
}