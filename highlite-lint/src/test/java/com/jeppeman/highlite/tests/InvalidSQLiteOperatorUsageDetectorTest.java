package com.jeppeman.highlite.tests;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.jeppeman.highlite.InvalidSQLiteOperatorUsageDetector;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static com.android.tools.lint.checks.infrastructure.TestLintTask.lint;
import static com.android.tools.lint.checks.infrastructure.TestFiles.java;

public class InvalidSQLiteOperatorUsageDetectorTest {
    private static final LintDetectorTest.TestFile SQLITE_TABLE = java(""
            + "package com.jeppeman.highlite;\n"
            + "\n"
            + "public @interface SQLiteTable {"
            + "}");

    private static final LintDetectorTest.TestFile SQLITE_OPERATOR = java(""
            + "package com.jeppeman.highlite;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "\n"
            + "public class SQLiteOperator {\n"
            + "\n"
            + "    public static SQLiteOperator from(final Context context, final Class<?> cls) {\n"
            + "        return new SQLiteOperator();\n"
            + "    }\n"
            + "\n"
            + "}"
    );

    private static final LintDetectorTest.TestFile INCORRECT_TEST = java(""
            + "package com.jeppeman.highlite;\n"
            + "\n"
            + "import com.jeppeman.highlite.SQLiteOperator;\n"
            + "import android.content.Context;\n"
            + "\n"
            + "public class Test {\n"
            + "  void test() {\n"
            + "    SQLiteOperator.from(null, String.class);\n"
            + "  }\n"
            + "}\n");

    private static final LintDetectorTest.TestFile CORRECT_TEST = java(""
            + "package com.jeppeman.highlite;\n"
            + "\n"
            + "import com.jeppeman.highlite.SQLiteOperator;\n"
            + "import android.content.Context;\n"
            + "\n"
            + "@SQLiteTable\n"
            + "public class Test {\n"
            + "  void test() {\n"
            + "    SQLiteOperator.from(null, Test.class);\n"
            + "  }\n"
            + "}\n");


    @Before
    public void setUp() throws Exception {
        final Properties p = new Properties(System.getProperties());
        final InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("sdktools.properties");
        p.load(is);
        p.setProperty("com.android.tools.lint.bindir", p.getProperty("sdkToolsDir"));
        System.setProperties(p);
    }

    @Test
    public void testIncorrectUsage() {
        lint()
                .files(SQLITE_OPERATOR, INCORRECT_TEST)
                .issues(InvalidSQLiteOperatorUsageDetector.ISSUE)
                .run()
                .expectErrorCount(1)
                .expect("src/com/jeppeman/highlite/Test.java:8: Error: java.lang.String must be annotated with @SQLiteTable to be used with SQLiteOperator.from [InvalidSQLiteOperatorUsage]\n"
                        + "    SQLiteOperator.from(null, String.class);\n"
                        + "                              ~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings");
    }

    @Test
    public void testCorrectUsage() {
        lint()
                .files(SQLITE_TABLE, SQLITE_OPERATOR, CORRECT_TEST)
                .issues(InvalidSQLiteOperatorUsageDetector.ISSUE)
                .run()
                .expectErrorCount(0);
    }
}