SQLiteProcessor
===

SQLiteProcessor is an SQLite library for Android that makes use of annotation processing to generate boilerplate for your SQLite operations.

Some features:

* No need to define subclasses of SQLiteOpenHelper. Automatic table creation and automatic column addition / deletion
* Query builder to not have to deal with the null-argument passing to the standard Android SQLite API.
* Easy to use API with blocking and non-blocking (using RxJava) versions of all database operations such as create, update, delete and fetching.

How to use
---
Annotate a class or a package with ```@SQLiteDatabaseHolder``` as follows:
```java
@SQLiteDatabaseHolder(databases = {
        @SQLiteDatabaseDescriptor(
                dbName = "myDatabase",
                dbVersion = 1,
                tables = {
                        MyClass.class,
                }
        )
})
public class ExampleApp extends Application {

}
```

or

```java
@SQLiteDatabaseHolder(databases = {
        @SQLiteDatabaseDescriptor(
                dbName = "myDatabase",
                dbVersion = 1,
                tables = {
                        MyClass.class,
                }
        )
})
package com.example.sqliteprocessor
```

The classes in the ```tables``` property must be classes annotated with ```@SQLiteTable``` as  follows:

```java
@SQLiteTable(tableName = "myTable")
public class MyClass {
    
    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    long id; // fields annotated with @SQLiteField need to be package local
    
    @SQLiteField(fieldName = "anotherFieldName")
    String name;
    
    @SQLiteField
    List<String> names;
}
```

This setup would generate code to automatically create the database ```myDatabase``` containing the table ```myTable``` with database fields ```id (INTEGER PRIMARY KEY AUTOINCREMENT)```, ```anotherFieldName (TEXT)``` and ```names (BLOB)```.

<b>Insertion</b>:
```java
final MyClass myClass = new MyClass();
myClass.name = "name";
myClass.names = Arrays.asList("name1", "name2");

SQLiteOperator.insertBlocking(context, myClass);
```

<b>Fetch and Update by id</b>:
```java
final MyClass myClass = SQLiteOperator.getSingleBlocking(context, MyClass.class, 1);
myClass.name = "anotherName";
SQLiteOperator.updateBlocking(context, myClass);
```

<b>Fetch and Update by query</b>:
```
final MyClass myClass = SQLiteOperator.getSingle(context, MyClass.class, );
myClass.name = "namer";
```
