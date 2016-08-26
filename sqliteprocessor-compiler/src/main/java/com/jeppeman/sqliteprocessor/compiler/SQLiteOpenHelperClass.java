package com.jeppeman.sqliteprocessor.compiler;

import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteTable;
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
        return CodeBlock.of("database.execSQL(\"$L\");\n",
                getCreateStatement(element, table));
    }

    private CodeBlock getUpgradeBlock(final Element element, final SQLiteTable table) {
        final String cursorVarName = table.tableName() + "Cursor",
                dbColsVarName = table.tableName() + "Cols";

        final CodeBlock.Builder alterStatements = CodeBlock.builder();
        for (final Element enclosed : element.getEnclosedElements()) {
            final SQLiteField field = enclosed.getAnnotation(SQLiteField.class);
            if (field == null) continue;

            alterStatements.beginControlFlow("if (!$L.contains(\"$L\"))", dbColsVarName,
                    getFieldName(enclosed, field))
                    .addStatement("database.execSQL(\"$L\")", getAlterStatement(enclosed, field,
                            table))
                    .endControlFlow()
                    .add("\n");
        }

        return CodeBlock.builder().addStatement(
                "final $T $L = database.rawQuery(\"PRAGMA table_info($L)\", null)",
                ClassName.bestGuess("android.database.Cursor"), cursorVarName,
                table.tableName())
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
                .add(alterStatements.build())
                .build();
    }

    private String getAlterStatement(final Element element,
                                     final SQLiteField field,
                                     final SQLiteTable table) {
        return "ALTER TABLE `"
                + table.tableName()
                + "` ADD COLUMN `"
                + getFieldName(element, field)
                + "` "
                + getFieldType(element, field);
    }

    private String getCreateStatement(final Element element, final SQLiteTable table) {
        final StringBuilder builder = new StringBuilder("CREATE TABLE " + table.tableName() + "(");

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
                .initializer("\"$L\"", mDatabaseName)
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

        //getClassName((TypeElement) element, packageName)
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
