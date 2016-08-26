package com.jeppeman.sqliteprocessor.compiler;

import com.google.auto.service.AutoService;
import com.jeppeman.sqliteprocessor.AutoIncrement;
import com.jeppeman.sqliteprocessor.PrimaryKey;
import com.jeppeman.sqliteprocessor.SQLiteField;
import com.jeppeman.sqliteprocessor.SQLiteGetter;
import com.jeppeman.sqliteprocessor.SQLiteSetter;
import com.jeppeman.sqliteprocessor.SQLiteTable;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
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

        for (final Map.Entry<String, Pair<Integer, Map<SQLiteTable, Element>>> entry :
                getTablesWithElements(roundEnv).entrySet()) {

            final JavaFile helperFile = new SQLiteOpenHelperClass(entry.getKey(),
                    entry.getValue().getValue(),
                    entry.getValue().getKey(),
                    mElementUtils).writeJava();

            try {
                helperFile.writeTo(mFiler);
            } catch (IOException e) {
//                error(tableElementEntry.getValue(),
//                        "Unable to write helper file for %s: %s",
//                        tableElementEntry.getValue().asType().toString(),
//                        e.getMessage());
            }

            for (final Map.Entry<SQLiteTable, Element> tableElementEntry :
                    entry.getValue().getValue().entrySet()) {
                final JavaFile daoFile = new DAOClass(entry.getKey(),
                        tableElementEntry.getKey(),
                        tableElementEntry.getValue(),
                        entry.getValue().getKey(),
                        mElementUtils).writeJava();

                try {
                    daoFile.writeTo(mFiler);
                } catch (IOException e) {
                    error(tableElementEntry.getValue(),
                            "Unable to write helper file for %s: %s",
                            tableElementEntry.getValue().asType().toString(),
                            e.getMessage());
                }
            }
        }

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

    private Map<String, Pair<Integer, Map<SQLiteTable, Element>>> getTablesWithElements(
            final RoundEnvironment roundEnv) {
        final Map<String, Pair<Integer, Map<SQLiteTable, Element>>> tables = new HashMap<>();
        for (final Element element : roundEnv.getElementsAnnotatedWith(SQLiteTable.class)) {
            final SQLiteTable table = element.getAnnotation(SQLiteTable.class);
            final Pair<Integer, Map<SQLiteTable, Element>> elementEntry =
                    tables.get(table.databaseName());
            if (elementEntry == null) {
                Map<SQLiteTable, Element> map = new HashMap<>();
                map.put(table, element);
                tables.put(table.databaseName(), new Pair<>(table.version(), map));
            } else if (table.version() > elementEntry.getKey()) {
                Map<SQLiteTable, Element> map = elementEntry.getValue();
                map.put(table, element);
                tables.put(table.databaseName(), new Pair<>(table.version(), map));
            } else {
                Map<SQLiteTable, Element> map = elementEntry.getValue();
                map.put(table, element);
                tables.put(table.databaseName(), new Pair<>(elementEntry.getKey(), map));
            }
        }

        return tables;
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