package com.jeppeman.liteomatic.tests;

import com.android.annotations.Nullable;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.utils.SdkUtils;
import com.google.common.collect.ImmutableList;
import com.jeppeman.liteomatic.InvalidSQLiteOperatorUsageDetector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.Properties;

public class InvalidSQLiteOperatorUsageDetectorTest extends LintDetectorTest {

    private static final String PATH_TEST_RESOURCES =
            "/src/test/java/com/jeppeman/liteomatic/";
    private static final String NO_WARNINGS = "No warnings.";

    @Override
    protected void setUp() throws Exception {
        Properties p = new Properties(System.getProperties());
        InputStream is = getClass().getClassLoader().getResourceAsStream("sdktools.properties");
        p.load(is);
        p.setProperty("com.android.tools.lint.bindir", p.getProperty("sdkToolsDir"));
        System.setProperties(p);
        super.setUp();
    }

    @Override
    public InputStream getTestResource(String relativePath, boolean expectExists) {
        String path = (PATH_TEST_RESOURCES + relativePath).replace('/', File.separatorChar);
        File file = new File(getTestDataRootDir(), path);
        if (file.exists()) {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                if (expectExists) {
                    fail("Could not find file " + relativePath);
                }
            }
        }
        return null;
    }

    @Nullable
    private File getTestDataRootDir() {
        CodeSource source = getClass().getProtectionDomain().getCodeSource();
        if (source != null) {
            URL location = source.getLocation();
            try {
                File classesDir = SdkUtils.urlToFile(location);
                return classesDir.getParentFile().getAbsoluteFile().getParentFile().getParentFile();
            } catch (MalformedURLException e) {
                fail(e.getLocalizedMessage());
            }
        }
        return null;
    }

    @Override
    protected Detector getDetector() {
        return new InvalidSQLiteOperatorUsageDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return ImmutableList.of(InvalidSQLiteOperatorUsageDetector.ISSUE);
    }

    public void testCorrectUsage() throws Exception {
        assertEquals(NO_WARNINGS, lintFiles("CorrectSQLiteOperatorUsage.java",
                "SQLiteOperator.java", "TestTable.java"));
    }

    public void testIncorrectUsage() throws Exception {
        final String output = lintFiles("IncorrectSQLiteOperatorUsage.java", "SQLiteOperator.java",
                "TestTable.java");
        assertNotSame(NO_WARNINGS, output);
        assertTrue(output.contains("1 errors, 0 warnings"));
    }
}