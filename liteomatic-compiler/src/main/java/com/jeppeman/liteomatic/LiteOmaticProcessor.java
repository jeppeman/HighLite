package com.jeppeman.liteomatic;

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
public class LiteOmaticProcessor extends AbstractProcessor {

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

        final List<String> databases = new ArrayList<>(),
                tables = new ArrayList<>();
        final Map<Element, JavaFile> helperFiles = new LinkedHashMap<>(),
                daoFiles = new LinkedHashMap<>();
        for (final Element element
                : roundEnv.getElementsAnnotatedWith(SQLiteDatabaseDescriptor.class)) {

            final SQLiteDatabaseDescriptor descriptor =
                    element.getAnnotation(SQLiteDatabaseDescriptor.class);

            if (databases.contains(descriptor.dbName())) {
                error(element, "The database " + descriptor.dbName() + " has already "
                        + "been defined");
                return true;
            } else {
                databases.add(descriptor.dbName());
            }

            List<? extends TypeMirror> mirrors = new ArrayList<>();
            try {
                descriptor.tables();
            } catch (MirroredTypesException e) {
                mirrors = e.getTypeMirrors();
            }

            for (final TypeMirror typeMirror : mirrors) {
                if (tables.contains(typeMirror.toString())) {
                    error(element, typeMirror.toString() + " is already specified as the table"
                            + " of a database");
                }

                tables.add(typeMirror.toString());

                final Element mirrorElem = mTypeUtils.asElement(typeMirror);
                if (mirrorElem.getAnnotation(SQLiteTable.class) == null) {
                    error(element, typeMirror.toString()
                            + " must be annotated with " + SQLiteTable.class.getName());
                    return true;
                }
            }

            final Map<SQLiteTable, Element> tablesForDatabase =
                    getTableElementMappingForDatabase(roundEnv, mirrors);

            final String packageName = mElementUtils
                    .getPackageOf(element)
                    .getQualifiedName()
                    .toString();

            helperFiles.put(element, new SQLiteOpenHelperClass(element, packageName,
                    descriptor.dbName(),
                    tablesForDatabase, descriptor.dbVersion(), mElementUtils,
                    mTypeUtils).writeJava());

            for (final Map.Entry<SQLiteTable, Element> entry : tablesForDatabase.entrySet()) {
                daoFiles.put(entry.getValue(), new SQLiteDAOClass(packageName,
                        descriptor.dbName(),
                        entry.getKey(), entry.getValue(), mElementUtils).writeJava());
            }
        }

        for (final Map.Entry<Element, JavaFile> helperFile : helperFiles.entrySet()) {
            try {
                helperFile.getValue().writeTo(mFiler);
            } catch (IOException e) {
                error(helperFile.getKey(), "Unable to generate helper file for %s: %s",
                        helperFile.getKey().asType().toString(), e.getMessage());
                return true;
            } catch (ProcessingException e) {
                error(e.getElement(), "Unable to generate helper file for %s: %s",
                        helperFile.getKey().asType().toString(), e.getMessage());
                return true;
            }
        }

        for (final Map.Entry<Element, JavaFile> daoFile : daoFiles.entrySet()) {
            try {
                daoFile.getValue().writeTo(mFiler);
            } catch (IOException e) {
                error(daoFile.getKey(), "Unable to generate DAO file for %s: %s",
                        daoFile.getKey().asType().toString(), e.getMessage());
                return true;
            } catch (ProcessingException e) {
                error(e.getElement(), "Unable to generate DAO file for %s: %s",
                        daoFile.getKey().asType().toString(), e.getMessage());
                return true;
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
        annotations.add(SQLiteTable.class);
        annotations.add(SQLiteDatabaseDescriptor.class);
        annotations.add(OnCreate.class);
        annotations.add(OnUpgrade.class);
        annotations.add(OnOpen.class);

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