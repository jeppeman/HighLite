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
                        MyClass2.class
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
                        MyClass2.class
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

```java
@SQLiteTable(tableName = "myTable2", autoCreate = false, autoAddColumns = false)
public class MyClass2 {

    @OnCreate
    public static void onCreate(SQLiteDatabase database) {
        // If you want to handle creation manually do so here
    }
    
    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // If you want to handle upgrading manually do so here
    }
    
    @SQLiteField
    @PrimaryKey
    @AutoIncrement
    long id;
    
    @SQLiteField
    String name;
    
    @SQLiteField
    List<String> names;
}
```

This setup would generate code to automatically create the database ```myDatabase``` containing the table ```myTable``` with database fields ```id (INTEGER PRIMARY KEY AUTOINCREMENT)```, ```anotherFieldName (TEXT)``` and ```names (BLOB)```.

Some operation examples:

<b>Insert</b>
```java
final MyClass myClass = new MyClass();
myClass.name = "name";
myClass.names = Arrays.asList("name1", "name2");

// Blocking
SQLiteOperator.insertBlocking(context, myClass);

// Non-blocking
SQLiteOperator.insert(context, myClass)
    .subscribe(new Completable.CompletableSubscriber() {
        @Override
        public void onCompleted() {
                       
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onSubscribe(Subscription d) {
                        
        }
});
```

<b>Fetch by id and Update</b>
```java
// Blocking
final MyClass myClass = SQLiteOperator.getSingleBlocking(context, MyClass.class, 1);
myClass.name = "anotherName";
SQLiteOperator.updateBlocking(context, myClass);

// Non-blocking
SQLiteOperator.getSingle(this, MyClass.class, 1)
    .subscribe(new SingleSubscriber<MyClass>() {
        @Override
        public void onSuccess(MyClass value) {
                        
        }

        @Override
        public void onError(Throwable error) {

        }
});
```
<b>Fetch by query</b>
```java
// Blocking
final List<MyClass> list = SQLiteOperator.getListBlocking(context, MyClass.class, SQLiteQuery.builder().where("id = ?", 1).build());

// Non-blocking
SQLiteOperator.getList(context, MyClass.class, SQLiteQuery.builder().where("id = ?", 1).build())
    .subscribe(new Subscriber<MyClass>() {
        @Override
        public void onCompleted() {
                        
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(MyClass myClass) {
            
        }
});
```
