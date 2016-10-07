package com.jeppeman.sqliteprocessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @author jesper
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SQLiteProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Elements mElementUtils;
    private Types mTypeUtils;
    private Filer mFiler;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        mElementUtils = processingEnv.getElementUtils();
        mTypeUtils = processingEnv.getTypeUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {

        for (final Element element
                : roundEnv.getElementsAnnotatedWith(SQLiteDatabaseHolder.class)) {

            final SQLiteDatabaseHolder dbAnno = element.getAnnotation(SQLiteDatabaseHolder.class);

            for (final SQLiteDatabaseDescriptor descriptor : dbAnno.databases()) {
                List<? extends TypeMirror> mirrors = new ArrayList<>();
                try {
                    descriptor.tables();
                } catch (MirroredTypesException e) {
                    mirrors = e.getTypeMirrors();
                }

                for (final TypeMirror typeMirror : mirrors) {
                    final Element mirrorElem = mTypeUtils.asElement(typeMirror);
                    if (mirrorElem.getAnnotation(SQLiteTable.class) == null) {
                        error(element, typeMirror.toString()
                                + " must be annotated with " + SQLiteTable.class.getName());
                        return true;
                    }
                }

                final Map<SQLiteTable, Element> tablesForDatabase =
                        getTableElementMappingForDatabase(roundEnv, mirrors);

                try {
                    final JavaFile helperFile = new SQLiteOpenHelperClass(descriptor.dbName(),
                            tablesForDatabase, descriptor.dbVersion(), mElementUtils,
                            mTypeUtils).writeJava();
                    helperFile.writeTo(mFiler);
                } catch (IOException e) {
                    error(element, "Unable to generate helper file for %s: %s",
                            element.asType().toString(), e.getMessage());
                    return true;
                } catch (ProcessingException e) {
                    error(e.getElement(), "Unable to generate helper file for %s: %s",
                            descriptor.dbName(), e.getMessage());
                    return true;
                }

                for (final Map.Entry<SQLiteTable, Element> entry : tablesForDatabase.entrySet()) {
                    try {
                        final JavaFile daoFile = new SQLiteDAOClass(descriptor.dbName(),
                                entry.getKey(), entry.getValue(), mElementUtils).writeJava();
                        daoFile.writeTo(mFiler);
                    } catch (IOException e) {
                        error(element, "Unable to generate DAO file for %s: %s",
                                element.asType().toString(), e.getMessage());
                        return true;
                    } catch (ProcessingException e) {
                        error(e.getElement(), "Unable to generate DAO file for %s: %s",
                                entry.getValue().asType().toString(), e.getMessage());
                        return true;
                    }
                }
            }
        }

        return true;
    }

    private Map<SQLiteTable, Element> getTableElementMappingForDatabase(
            final RoundEnvironment roundEnvironment,
            final List<? extends TypeMirror> tableMirrors) {
        final Map<SQLiteTable, Element> ret = new LinkedHashMap<>();

        for (final Element element : roundEnvironment.getElementsAnnotatedWith(SQLiteTable.class)) {
            final SQLiteTable tableAnno = element.getAnnotation(SQLiteTable.class);

            for (final TypeMirror mirror : tableMirrors) {
                if (!mTypeUtils.isSameType(mirror, element.asType())) continue;

                ret.put(tableAnno, element);
                break;
            }
        }

        return ret;
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

        annotations.add(ForeignKey.class);
        annotations.add(PrimaryKey.class);
        annotations.add(SQLiteField.class);
        annotations.add(SQLiteGetter.class);
        annotations.add(SQLiteSetter.class);
        annotations.add(SQLiteTable.class);
        annotations.add(SQLiteDatabaseHolder.class);
        annotations.add(SQLiteDatabaseDescriptor.class);
        annotations.add(OnCreate.class);
        annotations.add(OnUpgrade.class);

        return annotations;
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    @SuppressWarnings("unused")
    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void printMessage(
            Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        mMessager.printMessage(kind, message, element);
    }
}