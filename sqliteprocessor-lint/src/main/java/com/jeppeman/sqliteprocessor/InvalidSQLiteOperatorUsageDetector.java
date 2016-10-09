package com.jeppeman.sqliteprocessor;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.EnumSet;

import lombok.ast.AstVisitor;

public class InvalidSQLiteOperatorUsageDetector extends Detector implements Detector.JavaScanner {

    private static final String ISSUE_ID = "InvalidSQLiteOperatorUsage";
    private static final String ISSUE_TITLE = "Invalid usage of SQLiteOperator";
    private static final String ISSUE_BODY = "%s must be annotated with @SQLiteTable to be used"
            + " with SQLiteOperator.from";

    public static final Issue ISSUE = Issue.create(ISSUE_ID, ISSUE_TITLE, ISSUE_BODY,
            Category.CORRECTNESS, 6, Severity.ERROR,
            new Implementation(InvalidSQLiteOperatorUsageDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        final String name = file.getName();
        return !name.contains("_DAO");
    }

    @Override
    public EnumSet<Scope> getApplicableFiles() {
        return Scope.JAVA_FILE_SCOPE;
    }

    @Override
    public AstVisitor createJavaVisitor(final @NonNull JavaContext context) {
        return new FromMethodVisitor(context);
    }
}