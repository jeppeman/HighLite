package com.jeppeman.sqliteprocessor.compiler;

import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteTable;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by jesper on 2016-08-25.
 */
final class SQLiteDAOClass extends JavaWritableClass {

    private final TypeName mHelperClassName;
    private final SQLiteTable mTable;
    private final Element mElement;
    private final Elements mElementUtils;;

    SQLiteDAOClass(final TypeName helperClassName,
                   final SQLiteTable table,
                   final Element element,
                   final Elements elementUtils) {
        mHelperClassName = helperClassName;
        mTable = table;
        mElement = element;
        mElementUtils = elementUtils;
    }

    private MethodSpec buildGetContentValuesMethod() {
        final String contentValsVar = "contentValues";

        final CodeBlock.Builder putStatements = CodeBlock.builder();
        for (final Element enclosed : mElement.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            final String fieldType = getFieldType(enclosed, field),
                    fieldName = getFieldName(enclosed, field),
                    fieldNameVar = "fieldName" + fieldName,
                    fieldTypeVar = "fieldType" + fieldName;

            final CodeBlock.Builder putStatement = CodeBlock.builder();
            if (SQLiteFieldType.valueOf(fieldType) == SQLiteFieldType.BLOB) {
                putStatement.beginControlFlow("try")
                        .addStatement("final $T baos = new $T()", BYTE_ARRAY_OS, BYTE_ARRAY_OS)
                        .addStatement("final $T oos = new $T(baos)", OBJECT_OS, OBJECT_OS)
                        .addStatement("oos.writeObject(mTarget.$L)", enclosed.getSimpleName())
                        .addStatement("$L.put(\"$L\", baos.toByteArray())", contentValsVar,
                                fieldName)
                        .endControlFlow()
                        .beginControlFlow("catch ($T e)", IO_EXCEPTION)
                        .addStatement("throw new $T(e)", RUNTIME_EXCEPTION)
                        .endControlFlow();
            } else {
                putStatement.addStatement("$L.put(\"$L\", mTarget.$L)", contentValsVar, fieldName,
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

    private MethodSpec buildInsertMethod() {
        return MethodSpec.methodBuilder("insert")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addStatement("getWritableDatabase($L).insert(\"$L\", null, getContentValues())",
                        "context", mTable.tableName())
                .build();
    }

    @Override
    JavaFile writeJava() {
        final String packageName = mElementUtils
                .getPackageOf(mElement)
                .getQualifiedName()
                .toString(),
                className = getClassName((TypeElement) mElement, packageName);
        final ClassName cn = ClassName.get((TypeElement) mElement),
                helperClassName = ClassName.bestGuess(mHelperClassName + "_Generated");

        final FieldSpec targetField = FieldSpec.builder(cn, "mTarget", Modifier.PRIVATE,
                Modifier.FINAL)
                .build();

        final MethodSpec ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(cn, "target", Modifier.FINAL)
                .addStatement("mTarget = target")
                .build();

        final MethodSpec getReadableDatabaseMethod = MethodSpec.methodBuilder(
                "getReadableDatabase")
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .returns(SQLITE_DATABASE)
                .addStatement("return new $T(context).getReadableDatabase()",
                        helperClassName)
                .build();

        final MethodSpec getWritableDatabaseMethod = MethodSpec.methodBuilder(
                "getWritableDatabase")
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addModifiers(Modifier.PRIVATE)
                .returns(SQLITE_DATABASE)
                .addStatement("return new $T(context).getWritableDatabase()",
                        helperClassName)
                .build();

        final CodeBlock getReadableDatabaseStatement = CodeBlock.builder()
                .addStatement("getReadableDatabase()").build(),
                getWritableDatabaseStatement = CodeBlock.builder()
                        .addStatement("getWritableDatabase()").build();

        final MethodSpec updateMethod = MethodSpec.methodBuilder("update")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();

        final MethodSpec deleteMethod = MethodSpec.methodBuilder("delete")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();

        final MethodSpec queryMethod = MethodSpec.methodBuilder("query")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .build();

        final MethodSpec getSingleMethod = MethodSpec.methodBuilder("getSingle")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(cn)
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(TypeName.OBJECT, "id", Modifier.FINAL)
                .build();

        final MethodSpec getCustomMethod = MethodSpec.methodBuilder("getCustom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, cn))
                .addParameter(CONTEXT, "context", Modifier.FINAL)
                .addParameter(STRING, "customWhere", Modifier.FINAL)
                .build();

        final TypeSpec typeSpec = TypeSpec.classBuilder(
                ClassName.bestGuess(packageName
                        + "."
                        + className + "_DAO"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(SQLITE_DAO, cn))
                .addField(targetField)
                .addMethods(Arrays.asList(
                        ctor,
                        buildGetContentValuesMethod(),
                        getReadableDatabaseMethod,
                        getWritableDatabaseMethod,
                        buildInsertMethod(),
                        updateMethod,
                        deleteMethod,
                        queryMethod,
                        getSingleMethod,
                        getCustomMethod
                ))
                .build();

        return JavaFile.builder(packageName, typeSpec)
                .addFileComment("Generated code from SQLiteProcessor. Do not modify!")
                .build();
    }
}