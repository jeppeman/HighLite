package com.jeppeman.highlite.tests;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;

import static com.android.tools.lint.checks.infrastructure.TestFiles.java;

public class InvalidSQLiteOperatorUsageDetectorTest {
    private static final LintDetectorTest.TestFile BIND_TEST = java(""
            + "package com.example.highlite;\n"
            + "\n"
            + "import java.lang.annotation.ElementType;\n"
            + "import java.lang.annotation.Retention;\n"
            + "import java.lang.annotation.RetentionPolicy;\n"
            + "import java.lang.annotation.Target;\n"
            + "\n"
            + "@Retention(RetentionPolicy.SOURCE) @Target({ ElementType.FIELD, ElementType.METHOD })\n"
            + "public @interface BindTest {\n"
            + "  int value();\n"
            + "}\n");
}