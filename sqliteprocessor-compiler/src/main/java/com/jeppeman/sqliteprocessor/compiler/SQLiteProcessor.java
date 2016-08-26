package com.jeppeman.sqliteprocessor.compiler;

import com.google.auto.service.AutoService;
import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteFieldType;
import com.jeppeman.sqliteprocessor.SQLiteGetter;
import com.jeppeman.sqliteprocessor.SQLiteSetter;
import com.jeppeman.sqliteprocessor.SQLiteTable;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import javafx.util.Pair;

/**
 * Created by jesper on 2016-08-25.
 */
@AutoService(Processor.class)
public class SQLiteProcessor extends AbstractProcessor {

    private static final Map<SQLiteFieldType, List<Class<?>>> SQLITE_FIELD_CLASS_MAPPING;

    static {
        SQLITE_FIELD_CLASS_MAPPING = new HashMap<>();
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.INTEGER,
                new ArrayList<Class<?>>(Arrays.asList(
                        boolean.class,
                        int.class,
                        short.class,
                        long.class,
                        Boolean.class,
                        Integer.class,
                        Short.class,
                        Long.class)));
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.REAL,
                new ArrayList<Class<?>>(Arrays.asList(
                        float.class,
                        double.class,
                        Float.class,
                        Double.class)));
        SQLITE_FIELD_CLASS_MAPPING.put(SQLiteFieldType.TEXT, new ArrayList<Class<?>>(
                Collections.singletonList(String.class)));
    }

    private Elements mElementUtils;
    private Types mTypeUtils;
    private Filer mFiler;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        mElementUtils = processingEnv.getElementUtils();
        mTypeUtils = processingEnv.getTypeUtils();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {

        buildSQLiteHelperFiles(roundEnv);
        buildSQLiteModelFiles(roundEnv);

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> types = new LinkedHashSet<>();
        for (final Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        final Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

        annotations.add(AutoIncrement.class);
        annotations.add(PrimaryKey.class);
        annotations.add(SQLiteField.class);
        annotations.add(SQLiteGetter.class);
        annotations.add(SQLiteSetter.class);
        annotations.add(SQLiteTable.class);

        return annotations;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private void buildSQLiteModelFiles(final RoundEnvironment roundEnv) {

    }

    private void buildSQLiteHelperFiles(final RoundEnvironment roundEnv) {
        final Map<String, Pair<Integer, Map<SQLiteTable, Element>>> helperElements = new HashMap<>();
        for (final Element element : roundEnv.getElementsAnnotatedWith(SQLiteTable.class)) {
            final SQLiteTable table = element.getAnnotation(SQLiteTable.class);
            final Pair<Integer, Map<SQLiteTable, Element>> elementEntry =
                    helperElements.get(table.databaseName());
            if (elementEntry == null) {
                Map<SQLiteTable, Element> map = new HashMap<>();
                map.put(table, element);
                helperElements.put(table.databaseName(), new Pair<>(table.version(), map));
            } else if (table.version() > elementEntry.getKey()) {
                Map<SQLiteTable, Element> map = elementEntry.getValue();
                map.put(table, element);
                helperElements.put(table.databaseName(), new Pair<>(table.version(), map));
            } else {
                Map<SQLiteTable, Element> map = elementEntry.getValue();
                map.put(table, element);
                helperElements.put(table.databaseName(), new Pair<>(elementEntry.getKey(), map));
            }
        }

        for (final Map.Entry<String, Pair<Integer, Map<SQLiteTable, Element>>> entry :
                helperElements.entrySet()) {

            final FieldSpec colNameIndex = FieldSpec.builder(TypeName.INT,
                    "COL_NAME_INDEX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("1")
                    .build();

            final FieldSpec dbName = FieldSpec.builder(ClassName.get(String.class),
                    "DATABASE_NAME", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\"$L\"", entry.getKey())
                    .build();

            final FieldSpec dbVersion = FieldSpec.builder(TypeName.INT,
                    "DATABASE_VERSION", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L", entry.getValue().getKey())
                    .build();

            final MethodSpec ctor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.bestGuess("android.content.Context"), "context",
                            Modifier.FINAL)
                    .addCode("super(context, $L, null, $L);\n", dbName.name, dbVersion.name)
                    .build();

            String packageName = null;
            final CodeBlock.Builder codeBlockOnCreate = CodeBlock.builder(),
                    codeBlockOnUpgrade = CodeBlock.builder();

            for (final Map.Entry<SQLiteTable, Element> tableElementEntry :
                    entry.getValue().getValue().entrySet()) {

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
                    .addParameter(ClassName.bestGuess("android.database.sqlite.SQLiteDatabase"),
                            "database", Modifier.FINAL)
                    .addCode(codeBlockOnCreate.build())
                    .build();

            final MethodSpec onUpgradeMethod = MethodSpec.methodBuilder("onUpgrade")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .addParameter(ClassName.bestGuess("android.database.sqlite.SQLiteDatabase"),
                            "database", Modifier.FINAL)
                    .addParameter(TypeName.INT, "oldVersion", Modifier.FINAL)
                    .addParameter(TypeName.INT, "newVersion", Modifier.FINAL)
                    .addCode(codeBlockOnUpgrade.build())
                    .build();

            //getClassName((TypeElement) element, packageName)
            final TypeSpec typeSpec = TypeSpec.classBuilder(
                    entry.getKey() + "Helper")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(ClassName.bestGuess("android.database.sqlite.SQLiteOpenHelper"))
                    .addFields(Arrays.asList(colNameIndex, dbVersion, dbName))
                    .addMethod(ctor)
                    .addMethods(Arrays.asList(onCreateMethod, onUpgradeMethod))
                    .build();

            final JavaFile file = JavaFile.builder(packageName, typeSpec)
                    .addFileComment("Generated code from SQLiteProcessor. Do not modify!")
                    .build();

            try {
                file.writeTo(mFiler);
            } catch (IOException e) {
//                error(element, "Unable to write helper file for %s: %s", entry.getKey(),
//                        e.getMessage());
            }
        }
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
                .addStatement("final $T<String> $L = new $T<>()",
                        ClassName.bestGuess("java.util.List"),
                        dbColsVarName,
                        ClassName.bestGuess("java.util.ArrayList"))
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

    private static String getFieldName(final Element element, final SQLiteField field) {
        return field.value() == null || field.value().length() == 0
                ? element.getSimpleName().toString()
                : field.value();
    }

    private static SQLiteFieldType getFieldTypeFromClass(final String cls) {
        SQLiteFieldType ret = SQLiteFieldType.BLOB;
        for (final Map.Entry<SQLiteFieldType, List<Class<?>>> entry
                : SQLITE_FIELD_CLASS_MAPPING.entrySet()) {

            for (final Class<?> clazz : entry.getValue()) {
                if (clazz.getName().equals(cls)) {
                    ret = entry.getKey();
                    break;
                }
            }

            if (ret != SQLiteFieldType.BLOB) break;
        }

        return ret;
    }

    private String getFieldType(final Element element, final SQLiteField field) {
        try {
            return field.fieldType() != SQLiteFieldType.UNSPECIFIED
                    ? field.fieldType().toString()
                    : getFieldTypeFromClass(element.asType().toString()).toString();
        } catch (Exception e) {
            error(element, "Class not found: %s", e);
            return null;
        }
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }
}