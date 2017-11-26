package com.jeppeman.highlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.almworks.sqlite4java.SQLite;
import com.jeppeman.highlite.test.table.TestDatabase;
import com.jeppeman.highlite.test.table.TestNonSerializable;
import com.jeppeman.highlite.test.table.TestSerializable;
import com.jeppeman.highlite.test.table.TestTable;
import com.jeppeman.highlite.test.table.TestTable2;
import com.jeppeman.highlite.test.table.TestTable3;
import com.jeppeman.highlite.test.table.TestTable4;
import com.jeppeman.highlite.test.table.TestTable5;
import com.jeppeman.highlite.test.table.TestTable6;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SQLiteOperatorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @After
    public void finishComponentTesting() throws ClassNotFoundException {
        resetSingleton(Class.forName(TestDatabase.class.getCanonicalName() + "_OpenHelper"),
                "sInstance");
    }

    private void resetSingleton(Class clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private SQLiteOpenHelper getHelperInstance() throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (SQLiteOpenHelper) Class.forName(TestDatabase.class.getCanonicalName()
                + "_OpenHelper").getMethod("getInstance", Context.class).invoke(null, getContext());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidClassPassed() {
        SQLiteOperator.from(getContext(), ArrayList.class).getList();
    }

    @Test
    public void testSaveAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator.getSingle(1).executeBlocking();
        assertNull(table);
        operator.save(new TestTable()).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, table.id);
    }

    @Test
    public void testSaveAndGetSingleByRawQuery() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM test_table WHERE id = ?", 1)
                .executeBlocking();
        assertNull(table);
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM test_table WHERE id = ?", 1)
                .executeBlocking();
        assertNotNull(table);
        assertEquals(table.id, 1);
        assertEquals(table.testString, "123");
        assertEquals(table.testSerializable.testField, "test");
        assertEquals(Arrays.toString(newTable.testList.toArray()),
                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testSaveAndGetSingleByQueryBuilder() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator
                .getSingle()
                .withQuery(SQLiteQuery.builder().where("`id` = ?", 1).build())
                .executeBlocking();
        assertNull(table);
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        table = operator
                .getSingle()
                .withQuery(SQLiteQuery.builder().where("`id` = ?", 1).build())
                .executeBlocking();
        assertNotNull(table);
        assertEquals(table.id, 1);
        assertEquals(table.testString, "123");
        assertEquals(table.testSerializable.testField, "test");
        assertEquals(Arrays.toString(newTable.testList.toArray()),
                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testSaveAndGetListByRawQueryBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator
                .getList()
                .withRawQuery("SELECT * FROM test_table")
                .executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.unique = 1;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM test_table")
                .executeBlocking();
        assertEquals(1, list.size());
        newTable.id = 0;
        newTable.unique = 2;
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM test_table")
                .executeBlocking();
        assertEquals(2, list.size());
    }

    @Test
    public void testSaveAndGetListByQueryBuilderBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(1, list.size());
        newTable.testString = "1234";
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(0, list.size());
    }

    @Test
    public void testSaveAndGetFullListBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator.getList().executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.unique = 1;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(1, list.size());
        newTable.id = 0;
        newTable.unique = 2;
        operator.save(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(2, list.size());
    }

    @Test
    public void testAutoCreateTableDisabled() throws Exception {
        TestTable t = new TestTable();
        TestTable3 t3 = new TestTable3();
        t3.foreign = t;
        SQLiteOperator.from(getContext(), TestTable.class)
                .save(t)
                .executeBlocking();
        exception.expect(SQLiteException.class);
        SQLiteOperator.from(getContext(), TestTable3.class)
                .save(t3)
                .executeBlocking();
    }

    @Test
    public void testPrimaryKeyAsString() throws Exception {
        SQLiteOperator<TestTable5> operator = SQLiteOperator.from(getContext(), TestTable5.class);
        operator.save(new TestTable5("test")).executeBlocking();
        assertNotNull(operator.getSingle("test").executeBlocking());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveWithNonSerializableFields() throws Exception {
        final TestTable2 table2 = new TestTable2();
        table2.nonSerializable = new TestNonSerializable();
        SQLiteOperator.from(getContext(), TestTable2.class).save(table2).executeBlocking();
    }

    @Test
    public void testSave() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        final TestTable table = new TestTable();
        table.testString = "123";
        table.testList = Arrays.asList("1", "2", "3");
        table.testBoolean = true;
        table.unique = 1;
        table.testSerializable = new TestSerializable("test");
        assertEquals(0, table.id);
        operator.save(table).executeBlocking();
        assertEquals(1, table.id);
        table.id = 0;
        table.unique = 2;
        operator.save(table).executeBlocking();
        assertEquals(2, table.id);
        operator.save(table).executeBlocking();
        assertEquals(2, table.id);
    }

    @Test
    public void testUpdateAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        final TestTable table = new TestTable();
        table.testString = "123";
        table.testList = Arrays.asList("1", "2", "3");
        table.testBoolean = true;
        table.testSerializable = new TestSerializable("test");
        operator.save(table).executeBlocking();
        table.testString = "testString";
        table.testBoolean = false;
        assertEquals(1, operator.save(table).executeBlocking());
        TestTable fetched = operator.getSingle(1).executeBlocking();
        assertNotNull(fetched);
        assertEquals(fetched.testString, table.testString);
        assertEquals(fetched.testBoolean, table.testBoolean);
    }

    @Test
    public void testDeleteAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = new TestTable();
        operator.save(table).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, operator.delete(table).executeBlocking());
        table = operator.getSingle(1).executeBlocking();
        assertNull(table);
    }

    @Test(expected = SQLiteConstraintException.class)
    public void testFailingForeignKeyConstraint() throws Exception {
        SQLiteOperator<TestTable4> operator = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 table4 = new TestTable4();
        table4.foreignKey = new TestTable();
        operator.save(table4).executeBlocking();
    }

    @Test(expected = SQLiteConstraintException.class)
    public void testFailingUniqueConstraint() throws Exception {
        SQLiteOperator<TestTable4> operator = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 t1 = new TestTable4();
        t1.uniqueField = "notUnique";
        operator.save(t1).executeBlocking();
        TestTable4 t2 = new TestTable4();
        t2.uniqueField = "notUnique";
        operator.save(t2).executeBlocking();
    }

    @Test
    public void testRelationship() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable t1 = new TestTable();
        t1.testString = "testing";
        t1.testBoolean = true;
        t1.testDate = new Date();
        operator.save(t1).executeBlocking();
        SQLiteOperator<TestTable4> operator2 = SQLiteOperator.from(getContext(), TestTable4.class);
        t1 = operator.getSingle(1).executeBlocking();
        assertNotNull(t1);
        assertEquals(0, t1.table4Relation.size());
        TestTable4 related1 = new TestTable4();
        related1.foreignKey = t1;
        TestTable4 related2 = new TestTable4();
        related2.foreignKey = t1;
        operator2.save(related1, related2).executeBlocking();
        t1 = operator.getSingle(1).executeBlocking();
        assertNotNull(t1);
        assertEquals(2, t1.table4Relation.size());
    }

    @Test(expected = SQLiteConstraintException.class)
    public void testNotNullFailed() throws Exception {
        SQLiteOperator<TestTable6> operator = SQLiteOperator.from(getContext(), TestTable6.class);
        operator.save(new TestTable6("test")).executeBlocking();
    }

    @Test
    public void testNotNull() throws Exception {
        SQLiteOperator<TestTable6> operator = SQLiteOperator.from(getContext(), TestTable6.class);
        TestTable6 table = new TestTable6("test");
        table.notNullString = "not null";
        operator.save(table).executeBlocking();
    }

    @Test
    public void testRespectedForeignKeyConstraintAndCascade() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable testTable = new TestTable();
        operator.save(testTable).executeBlocking();
        SQLiteOperator<TestTable4> operator2 = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 testTable4 = new TestTable4();
        testTable4.foreignKey = testTable;
        operator2.save(testTable4).executeBlocking();
        assertNotNull(operator2.getSingle(1).executeBlocking());
        operator.delete(testTable).executeBlocking();
        assertNull(operator.getSingle(1).executeBlocking());
        assertNull(operator2.getSingle(1).executeBlocking());
    }

    @Test
    public void testOnUpgradeWithColumnChange() throws Exception {
        getHelperInstance()
                .getReadableDatabase()
                .execSQL("CREATE TABLE testTable3 ("
                        + "    `xx` INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "    `str` TEXT,"
                        + "    `foreign` INTEGER,"
                        + "    FOREIGN KEY(`foreign`) REFERENCES test_table(`unique`)"
                        + ");"
                );

        SQLiteOperator<TestTable3> operator = SQLiteOperator.from(getContext(), TestTable3.class);
        TestTable3 t = new TestTable3();
        operator.save(t).executeBlocking();
        operator.delete(t).executeBlocking();
        getHelperInstance().onUpgrade(getHelperInstance().getWritableDatabase(), 1, 2);
        exception.expect(SQLiteConstraintException.class);
        operator.save(new TestTable3()).executeBlocking();
    }

    @Test
    public void testOnUpgradeValuePersistence() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable testTable = new TestTable();
        testTable.testString = "Persisting across upgrade?";
        operator.save(testTable).executeBlocking();

        assertNotNull(operator.getSingle(1).executeBlocking());

        getHelperInstance().onUpgrade(getHelperInstance().getWritableDatabase(), 1, 2);

        assertNotNull(operator
                .getSingle()
                .withQuery(
                        SQLiteQuery
                                .builder()
                                .where("`testFieldName` = ?", "Persisting across upgrade?")
                                .build()
                )
                .executeBlocking()
        );
    }

    @Test
    public void testOnUpgradeWithAddAndDeleteColumn() throws Exception {
        getHelperInstance()
                .getWritableDatabase()
                .execSQL("DROP TABLE test_table;");

        getHelperInstance()
                .getWritableDatabase()
                .execSQL("CREATE TABLE test_table (\n" +
                        "      `id` INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "      `testFieldName` TEXT,\n" +
                        "      `testList` BLOB,\n" +
                        "      `testBoolean` INTEGER,\n" +
                        "      `testSerializable` BLOB,\n" +
                        "      `upgradeDeleteTester` TEXT\n" +
                        "    );");

        Cursor testTableCursor = getHelperInstance()
                .getReadableDatabase()
                .rawQuery("PRAGMA table_info(test_table)", null);
        final List<String> testTableCols = new ArrayList<>();
        if (testTableCursor.moveToFirst()) {
            do {
                testTableCols.add(testTableCursor.getString(1));
            } while (testTableCursor.moveToNext());
        }
        testTableCursor.close();

        final String upgradeAddColName = "upgradeAddTester",
                upgradeDeleteColName = "upgradeDeleteTester";

        assertTrue(!testTableCols.contains(upgradeAddColName));
        assertTrue(testTableCols.contains(upgradeDeleteColName));

        getHelperInstance().onUpgrade(getHelperInstance().getWritableDatabase(), 1, 2);
        testTableCursor = getHelperInstance()
                .getReadableDatabase()
                .rawQuery("PRAGMA table_info(test_table)", null);
        testTableCols.clear();

        if (testTableCursor.moveToFirst()) {
            do {
                testTableCols.add(testTableCursor.getString(1));
            } while (testTableCursor.moveToNext());
        }
        testTableCursor.close();

        assertTrue(testTableCols.contains(upgradeAddColName));
        assertTrue(!testTableCols.contains(upgradeDeleteColName));
    }
}