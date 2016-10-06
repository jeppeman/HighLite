package com.jeppeman.sqliteprocessor;

import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.TextFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.ast.ClassLiteral;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodInvocation;

import static com.jeppeman.sqliteprocessor.InvalidSQLiteOperatorUsageDetector.ISSUE;

class FromMethodVisitor extends ForwardingAstVisitor {

    private static final int PARAM_INDEX = 1;
    private static final String CLASS_SQLITEOPERATOR =
            "com.jeppeman.sqliteprocessor.SQLiteOperator";
    private static final String ANNOTATION_TYPE_LONG = "com.jeppeman.sqliteprocessor.SQLiteTable";
    private static final String ANNOTATION_TYPE_SHORT = "SQLiteTable";
    private static final String METHOD_NAME = "from";

    private final JavaContext mContext;

    FromMethodVisitor(final JavaContext context) {
        this.mContext = context;
    }

    @Override
    public boolean visitMethodInvocation(final MethodInvocation node) {
        final JavaParser.ResolvedNode resolvedNode = mContext.resolve(node);
        if (!(resolvedNode instanceof JavaParser.ResolvedMethod)) {
            return super.visitMethodInvocation(node);
        }
        final JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolvedNode;

        if (!(method.getContainingClass().getName().equals(CLASS_SQLITEOPERATOR)
                && method.getName().equals(METHOD_NAME))) {
            return super.visitMethodInvocation(node);
        }

        int iterator = 0;
        for (final Expression arg : node.astArguments()) {
            if (iterator++ < PARAM_INDEX) continue;
            if (iterator - 1 > PARAM_INDEX) break;

            final ClassLiteral classLiteral = (ClassLiteral) arg;
            if (classLiteral == null) continue;

            JavaParser.TypeDescriptor typeDesc = mContext.getType(classLiteral.rawTypeReference());

            if (typeDesc != null) {
                boolean found = false;
                for (final JavaParser.ResolvedAnnotation a
                        : typeDesc.getTypeClass().getAnnotations()) {
                    if (ANNOTATION_TYPE_SHORT.equals(a.getName())
                            || ANNOTATION_TYPE_LONG.equals(a.getName())) {
                        found = true;
                        break;
                    }
                }

                if (!found && !mContext.isSuppressedWithComment(node, ISSUE)) {
                    mContext.report(ISSUE, mContext.getLocation(arg),
                            String.format(ISSUE.getExplanation(TextFormat.TEXT),
                                    typeDesc.getName()));
                    return true;
                }
            } else {
                // TODO: Submit bug report to google about having to do this instead of
                // TODO: mContext.getType(arg) when JavaParser is LombokPsiParser and
                // TODO: PsiElement is of type PsiTypeElement
                final Pattern pattern = Pattern.compile("<.+>");
                final String s = mContext.getType(arg).getName();
                final Matcher m = pattern.matcher(s);
                while (m.find()) {
                    final JavaParser.ResolvedClass resolvedClass = mContext.findClass(
                            s.substring(m.start() + 1, m.end() - 1));

                    if (resolvedClass == null) continue;

                    boolean found = false;
                    for (final JavaParser.ResolvedAnnotation a : resolvedClass.getAnnotations()) {
                        if (ANNOTATION_TYPE_SHORT.equals(a.getName())
                                || ANNOTATION_TYPE_LONG.equals(a.getName())) {
                            found = true;
                            break;
                        }
                    }

                    if (!found && !mContext.isSuppressedWithComment(node, ISSUE)) {
                        mContext.report(ISSUE, mContext.getLocation(arg),
                                String.format(ISSUE.getExplanation(TextFormat.TEXT),
                                        resolvedClass.getName()));
                        return true;
                    }
                }
            }
        }

        return super.visitMethodInvocation(node);
    }
}