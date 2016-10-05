package com.jeppeman.sqliteprocessor;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SQLiteOperatorTest {

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidClassPassed() {
        //SQLiteOperator.insertBlocking(getContext(), new ArrayList<>());
    }

    @Test
    public void testGetSingleById() throws Exception {
//        TestTable table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class, 1);
//        assertNull(table);
//        SQLiteOperator.insertBlocking(getContext(), new TestTable());
//        table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class, 1);
//        assertNotNull(table);
//        assertEquals(1, table.id);
    }

    @Test
    public void testGetSingleByRawQuery() throws Exception {
//        TestTable table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class,
//                "SELECT * FROM testTable WHERE id = ?", 1);
//        assertNull(table);
//        TestTable newTable = new TestTable();
//        newTable.testSerializable = new TestSerializable("test");
//        newTable.testString = "123";
//        newTable.testBoolean = true;
//        newTable.testList = Arrays.asList("1", "2", "3");
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class,
//                "SELECT * FROM testTable WHERE id = ?", 1);
//        assertNotNull(table);
//        assertEquals(table.id, 1);
//        assertEquals(table.testString, "123");
//        assertEquals(table.testSerializable.testField, "test");
//        assertEquals(Arrays.toString(newTable.testList.toArray()),
//                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testGetSingleByQueryBuilder() throws Exception {
//        TestTable table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class,
//                SQLiteQuery.builder().where("id = ?", 1).build());
//        assertNull(table);
//        TestTable newTable = new TestTable();
//        newTable.testSerializable = new TestSerializable("test");
//        newTable.testString = "123";
//        newTable.testBoolean = true;
//        newTable.testList = Arrays.asList("1", "2", "3");
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class,
//                SQLiteQuery.builder().where("id = ?", 1).build());
//        assertNotNull(table);
//        assertEquals(table.id, 1);
//        assertEquals(table.testString, "123");
//        assertEquals(table.testSerializable.testField, "test");
//        assertEquals(Arrays.toString(newTable.testList.toArray()),
//                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testGetListByRawQueryBlocking() throws Exception {
//        List<TestTable> list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                "SELECT * FROM testTable");
//        assertNotNull(list);
//        assertEquals(0, list.size());
//        TestTable newTable = new TestTable();
//        newTable.testSerializable = new TestSerializable("test");
//        newTable.testString = "123";
//        newTable.testBoolean = true;
//        newTable.testList = Arrays.asList("1", "2", "3");
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                "SELECT * FROM testTable");
//        assertEquals(1, list.size());
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                "SELECT * FROM testTable");
//        assertEquals(2, list.size());
    }

    @Test
    public void testGetListByQueryBuilderBlocking() throws Exception {
//        List<TestTable> list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                SQLiteQuery.builder().where("testFieldName = ?", "123").build());
//        assertNotNull(list);
//        assertEquals(0, list.size());
//        TestTable newTable = new TestTable();
//        newTable.testSerializable = new TestSerializable("test");
//        newTable.testString = "123";
//        newTable.testBoolean = true;
//        newTable.testList = Arrays.asList("1", "2", "3");
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                SQLiteQuery.builder().where("testFieldName = ?", "123").build());
//        assertEquals(1, list.size());
//        newTable.testString = "1234";
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getListBlocking(getContext(), TestTable.class,
//                SQLiteQuery.builder().where("testFieldName = ?", "123").build());
//        assertEquals(1, list.size());
    }

    @Test
    public void testGetFullListBlocking() throws Exception {
//        List<TestTable> list = SQLiteOperator.getFullListBlocking(getContext(), TestTable.class);
//        assertNotNull(list);
//        assertEquals(0, list.size());
//        TestTable newTable = new TestTable();
//        newTable.testSerializable = new TestSerializable("test");
//        newTable.testString = "123";
//        newTable.testBoolean = true;
//        newTable.testList = Arrays.asList("1", "2", "3");
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getFullListBlocking(getContext(), TestTable.class);
//        assertEquals(1, list.size());
//        SQLiteOperator.insertBlocking(getContext(), newTable);
//        list = SQLiteOperator.getFullListBlocking(getContext(), TestTable.class);
//        assertEquals(2, list.size());
    }

    @Test(expected = RuntimeException.class)
    public void testAutoCreateTableDisabled() throws Exception {
//        SQLiteOperator.insertBlocking(getContext(), new TestTable3());
    }

    @Test(expected = RuntimeException.class)
    public void testInsertWithNonSerializableFields() throws Exception {
//        final TestTable2 table2 = new TestTable2();
//        table2.nonSerializable = new TestNonSerializable();
//        SQLiteOperator.insertBlocking(getContext(), table2);
    }

    @Test
    public void testInsert() throws Exception {
//        final TestTable table = new TestTable();
//        table.testString = "123";
//        table.testList = Arrays.asList("1", "2", "3");
//        table.testBoolean = true;
//        table.testSerializable = new TestSerializable("test");
//        assertEquals(0, table.id);
//        SQLiteOperator.insertBlocking(getContext(), table);
//        assertEquals(1, table.id);
//        SQLiteOperator.insertBlocking(getContext(), table);
//        assertEquals(2, table.id);
    }

    @Test
    public void testUpdateBlocking() throws Exception {
//        final TestTable table = new TestTable();
//        table.testString = "123";
//        table.testList = Arrays.asList("1", "2", "3");
//        table.testBoolean = true;
//        table.testSerializable = new TestSerializable("test");
//        SQLiteOperator.insertBlocking(getContext(), table);
//        table.testString = "testString";
//        table.testBoolean = false;
//        SQLiteOperator.updateBlocking(getContext(), table);
//        TestTable fetched = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class, 1);
//        assertNotNull(fetched);
//        assertEquals(fetched.testString, table.testString);
//        assertEquals(fetched.testBoolean, table.testBoolean);
    }

    @Test
    public void testDeleteBlocking() throws Exception {
//        TestTable table = new TestTable();
//        SQLiteOperator.insertBlocking(getContext(), table);
//        table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class, 1);
//        assertNotNull(table);
//        SQLiteOperator.deleteBlocking(getContext(), table);
//        table = SQLiteOperator.getSingleBlocking(getContext(), TestTable.class, 1);
//        assertNull(table);
    }
}