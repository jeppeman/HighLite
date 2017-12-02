package com.jeppeman.highlite;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.ArrayList;
import java.util.List;

public class InvalidSQLiteOperatorUsageDetector extends Detector implements Detector.UastScanner {

    private static final String ISSUE_ID = "InvalidSQLiteOperatorUsage";
    private static final String ISSUE_TITLE = "Invalid usage of SQLiteOperator";
    private static final String ISSUE_BODY = "%s must be annotated with @SQLiteTable to be used"
            + " with SQLiteOperator.from";

    public static final Issue ISSUE = Issue.create(ISSUE_ID, ISSUE_TITLE, ISSUE_BODY,
            Category.CORRECTNESS, 6, Severity.ERROR,
            new Implementation(InvalidSQLiteOperatorUsageDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        final List<Class<? extends UElement>> ret = new ArrayList<>();
        ret.add(UClass.class);
        return ret;
    }

    @Override
    public UElementHandler createUastHandler(final JavaContext context) {
        return new UElementHandler() {
            @Override
            public void visitClass(UClass uClass) {
                uClass.accept(new FromMethodVisitor(context));
            }
        };
    }
}