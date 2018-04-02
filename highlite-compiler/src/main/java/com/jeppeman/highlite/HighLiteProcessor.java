package com.jeppeman.highlite;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.AbstractMap;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @author jesper
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class HighLiteProcessor extends AbstractProcessor {

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

        for (final Element element : roundEnv.getElementsAnnotatedWith(SQLiteColumn.class)) {
            final SQLiteColumn field = element.getAnnotation(SQLiteColumn.class);
            if (field.primaryKey().enabled() && field.foreignKey().enabled()) {
                error(element, String.format("%s can't have both %s and %s set to enabled",
                        SQLiteColumn.class.getCanonicalName(), PrimaryKey.class.getCanonicalName(),
                        ForeignKey.class.getCanonicalName()));
                return true;
            }

            final SQLiteRelationship relationship = element.getAnnotation(SQLiteRelationship.class);
            if (relationship != null) {
                error(element, String.format("Fields are not allowed to be annotated with both"
                                + "%s and %s", SQLiteColumn.class.getCanonicalName(),
                        SQLiteRelationship.class.getCanonicalName()));
            }
        }

        final List<String> databases = new ArrayList<>();
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

            final AbstractMap.SimpleEntry<Map<Element, SQLiteTable>, Boolean> tablesForDatabase =
                    getTableElementMappingForDatabase(roundEnv, element);
            if (tablesForDatabase.getValue()) {
                return true;
            }

            final String packageName = mElementUtils
                    .getPackageOf(element)
                    .getQualifiedName()
                    .toString();

            helperFiles.put(element, new SQLiteOpenHelperClass(element, packageName,
                    descriptor.dbName(),
                    tablesForDatabase.getKey(), descriptor.dbVersion(), mElementUtils,
                    mTypeUtils).writeJava());

            for (final Map.Entry<Element, SQLiteTable> entry
                    : tablesForDatabase.getKey().entrySet()) {
                daoFiles.put(entry.getKey(), new SQLiteDAOClass(packageName, descriptor.dbName(),
                        entry.getValue(), entry.getKey(), mElementUtils, mTypeUtils).writeJava());
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

    private AbstractMap.SimpleEntry<Map<Element, SQLiteTable>,
            Boolean> getTableElementMappingForDatabase(
            final RoundEnvironment roundEnvironment,
            final Element databaseElement) {
        final AbstractMap.SimpleEntry<Map<Element, SQLiteTable>, Boolean> ret =
                new AbstractMap.SimpleEntry<Map<Element, SQLiteTable>, Boolean>(
                        new LinkedHashMap<Element, SQLiteTable>(), false);

        final List<String> tableNamesAdded = new ArrayList<>();
        for (final Element element : roundEnvironment.getElementsAnnotatedWith(SQLiteTable.class)) {
            if (element.getModifiers().contains(Modifier.ABSTRACT)
                    || element.getKind().equals(ElementKind.INTERFACE)) continue;

            final SQLiteTable tableAnno = element.getAnnotation(SQLiteTable.class);

            TypeMirror mirror = null;
            try {
                tableAnno.database();
            } catch (MirroredTypeException e) {
                mirror = e.getTypeMirror();
            }

            final SQLiteDatabaseDescriptor dbAnno = mTypeUtils.asElement(mirror)
                    .getAnnotation(SQLiteDatabaseDescriptor.class);
            if (dbAnno == null) {
                error(element, String.format("The database class must be annotated with %s",
                        SQLiteDatabaseDescriptor.class.getCanonicalName()));
                ret.setValue(true);
                return ret;
            }

            final String tableName = JavaWritableClass.getTableName(element);

            if (mTypeUtils.isSameType(mirror, databaseElement.asType())) {
                if (tableNamesAdded.contains(tableName)) {
                    error(element, String.format("The table %s was already defined for database %s",
                            tableName, dbAnno.dbName()));
                    ret.setValue(true);
                    return ret;
                }

                tableNamesAdded.add(tableName);
                ret.getKey().put(element, tableAnno);
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
        annotations.add(SQLiteColumn.class);
        annotations.add(SQLiteTable.class);
        annotations.add(SQLiteDatabaseDescriptor.class);
        annotations.add(OnCreate.class);
        annotations.add(OnUpgrade.class);
        annotations.add(OnOpen.class);
        annotations.add(SQLiteRelationship.class);

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