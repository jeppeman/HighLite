package com.jeppeman.highlite;

import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.TextFormat;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.java.JavaUClassLiteralExpression;
import org.jetbrains.uast.java.JavaUCompositeQualifiedExpression;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import static com.jeppeman.highlite.InvalidSQLiteOperatorUsageDetector.ISSUE;

class FromMethodVisitor extends AbstractUastVisitor {

    private static final int PARAM_INDEX = 1;
    private static final String CLASS_SQLITE_OPERATOR = "SQLiteOperator";
    private static final String ANNOTATION_TYPE_LONG = "com.jeppeman.highlite.SQLiteTable";
    private static final String METHOD_NAME = "from";

    private final JavaContext mContext;

    FromMethodVisitor(final JavaContext context) {
        this.mContext = context;
    }

    @Override
    public boolean visitCallExpression(UCallExpression node) {
        if (!METHOD_NAME.equals(node.getMethodName())) return false;

        final JavaUCompositeQualifiedExpression exp =
                (JavaUCompositeQualifiedExpression) node.getUastParent();
        if (exp == null
                || !CLASS_SQLITE_OPERATOR.equals(exp.receiver.toString())
                || node.getValueArgumentCount() != 2) return false;

        final JavaUClassLiteralExpression classLiteral =
                (JavaUClassLiteralExpression) node.getValueArguments().get(PARAM_INDEX);

        if (classLiteral == null) return false;

        final PsiClass psiClass = mContext.getEvaluator().findClass(classLiteral.toString());

        if (psiClass == null
                || psiClass.getModifierList() == null) return false;

        boolean found = false;
        for (final PsiAnnotation annotation : psiClass.getModifierList().getAnnotations()) {
            if (!ANNOTATION_TYPE_LONG.equals(annotation.getQualifiedName())) continue;

            found = true;
            break;
        }

        if (!found) {
            mContext.report(ISSUE, mContext.getLocation(classLiteral),
                    String.format(ISSUE.getExplanation(TextFormat.TEXT), classLiteral));
            return true;
        }

        return false;
    }
}