package com.jeppeman.liteomatic;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.jeppeman.liteomatic.test.table.TestDatabase;
import com.jeppeman.liteomatic.test.table.TestNonSerializable;
import com.jeppeman.liteomatic.test.table.TestSerializable;
import com.jeppeman.liteomatic.test.table.TestTable;
import com.jeppeman.liteomatic.test.table.TestTable2;
import com.jeppeman.liteomatic.test.table.TestTable3;
import com.jeppeman.liteomatic.test.table.TestTable4;
import com.jeppeman.liteomatic.test.table.TestTable5;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SQLiteOperatorTest {

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidClassPassed() {
        SQLiteOperator.from(getContext(), ArrayList.class).getList();
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

    @Test
    public void testInsertAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator.getSingle(1).executeBlocking();
        assertNull(table);
        operator.insert(new TestTable()).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, table.id);
    }

    @Test
    public void testInsertAndGetSingleByRawQuery() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM testTable WHERE id = ?", 1)
                .executeBlocking();
        assertNull(table);
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.insert(newTable).executeBlocking();
        table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM testTable WHERE id = ?", 1)
                .executeBlocking();
        assertNotNull(table);
        assertEquals(table.id, 1);
        assertEquals(table.testString, "123");
        assertEquals(table.testSerializable.testField, "test");
        assertEquals(Arrays.toString(newTable.testList.toArray()),
                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testInsertAndGetSingleByQueryBuilder() throws Exception {
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
        operator.insert(newTable).executeBlocking();
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
    public void testInsertAndGetListByRawQueryBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.insert(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertEquals(1, list.size());
        operator.insert(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertEquals(2, list.size());
    }

    @Test
    public void testInsertAndGetListByQueryBuilderBlocking() throws Exception {
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
        operator.insert(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(1, list.size());
        newTable.testString = "1234";
        operator.insert(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(1, list.size());
    }

    @Test
    public void testInsertAndGetFullListBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator.getList().executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.insert(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(1, list.size());
        operator.insert(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(2, list.size());
    }

    @Test(expected = SQLiteException.class)
    public void testAutoCreateTableDisabled() throws Exception {
        SQLiteOperator.from(getContext(), TestTable3.class)
                .insert(new TestTable3())
                .executeBlocking();
    }

    @Test
    public void testPrimaryKeyAsString() throws Exception {
        SQLiteOperator<TestTable5> operator = SQLiteOperator.from(getContext(), TestTable5.class);
        operator.insert(new TestTable5("test")).executeBlocking();
        assertNotNull(operator.getSingle("test").executeBlocking());
    }

    @Test(expected = RuntimeException.class)
    public void testInsertWithNonSerializableFields() throws Exception {
        final TestTable2 table2 = new TestTable2();
        table2.nonSerializable = new TestNonSerializable();
        SQLiteOperator.from(getContext(), TestTable2.class).insert(table2).executeBlocking();
    }

    @Test
    public void testInsert() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        final TestTable table = new TestTable();
        table.testString = "123";
        table.testList = Arrays.asList("1", "2", "3");
        table.testBoolean = true;
        table.testSerializable = new TestSerializable("test");
        assertEquals(0, table.id);
        operator.insert(table).executeBlocking();
        assertEquals(1, table.id);
        operator.insert(table).executeBlocking();
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
        operator.insert(table).executeBlocking();
        table.testString = "testString";
        table.testBoolean = false;
        assertEquals(1, operator.update(table).executeBlocking());
        TestTable fetched = operator.getSingle(1).executeBlocking();
        assertNotNull(fetched);
        assertEquals(fetched.testString, table.testString);
        assertEquals(fetched.testBoolean, table.testBoolean);
    }

    @Test
    public void testDeleteAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = new TestTable();
        operator.insert(table).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, operator.delete(table).executeBlocking());
        table = operator.getSingle(1).executeBlocking();
        assertNull(table);
    }

    @Test(expected = SQLiteException.class)
    public void testFailingForeignKeyConstraint() {
        SQLiteOperator<TestTable4> operator = SQLiteOperator.from(getContext(), TestTable4.class);
        operator.insert(new TestTable4()).executeBlocking();
    }

    @Test
    public void testRespectedForeignKeyConstraintAndCascade() {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable testTable = new TestTable();
        operator.insert(testTable).executeBlocking();
        SQLiteOperator<TestTable4> operator2 = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 testTable4 = new TestTable4();
        testTable4.foreignKey = 1;
        operator2.insert(testTable4).executeBlocking();
        assertNotNull(operator2.getSingle(1).executeBlocking());
        operator.delete(testTable).executeBlocking();
        assertNull(operator2.getSingle(1).executeBlocking());
    }

    @Test
    public void testOnUpgradeWithAddAndDeleteColumnAndValuePersistence() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        operator.insert(new TestTable()).executeBlocking();
        Cursor testTableCursor = getHelperInstance()
                .getReadableDatabase()
                .rawQuery("PRAGMA table_info(testTable)", null);
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
                .rawQuery("PRAGMA table_info(testTable)", null);
        testTableCols.clear();

        if (testTableCursor.moveToFirst()) {
            do {
                testTableCols.add(testTableCursor.getString(1));
            } while (testTableCursor.moveToNext());
        }
        testTableCursor.close();

        assertTrue(testTableCols.contains(upgradeAddColName));
        assertTrue(!testTableCols.contains(upgradeDeleteColName));
        assertNotNull(operator.getSingle(1).executeBlocking());
    }
}