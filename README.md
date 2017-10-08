[![Build Status](https://travis-ci.org/jeppeman/LiteOmatic.svg?branch=master)](https://travis-ci.org/jeppeman/LiteOmatic)

LiteOmatic
===

LiteOmatic is an SQLite library for Android that makes use of annotation processing to generate boilerplate for your SQLite operations.

Key features:

* No need to define subclasses of SQLiteOpenHelper; automates table creation, table deletion and table upgrades
* Query builder that removes the need to have to deal with the null-argument passing to the standard Android SQLite API.
* Easy to use API with blocking and non-blocking (using RxJava) operations.
* Fast and thread safe

Getting started
---
```groovy
dependencies {
    compile 'com.jeppeman:liteomatic:1.0-beta1'
    annotationProcessor 'com.jeppeman:liteomatic-compiler:1.0-beta1'
}

```

Example usages
---
Annotate a class with ```@SQLiteDatabaseDescriptor``` as follows:
```java

@SQLiteDatabaseDescriptor(
    dbName = "myDatabase",
    dbVersion = 1
)
public class MyDatabase {
    @OnCreate
    public static void onOpen(SQLiteDatabase db) {
        ...
    }
    
    // 
    @OnCreate
    public static void onCreate(SQLiteDatabase db) {
        ...
    }
    
    // 
    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ...
    }
}
```

Then define a class for a table

```java
@SQLiteTable(database = MyDatabase.class, tableName = "myTable")
public class MyClass {
    
    @SQLiteField
    @PrimaryKey
    long id; // fields annotated with @SQLiteField need to be package local
    
    @SQLiteField(fieldName = "anotherFieldName")
    String name;
    
    @SQLiteField
    List<String> names;
}
```

Insert an object:
---
```java
SQLiteOperator<MyTable> operator = SQLiteOperator.from(getContext(), MyTable.class);
final MyTable myTableObject = new MyTable();
myTableObject.name = "name";
myTableObject.names = Arrays.asList("name1", "name2");

// Blocking
operator.insert(myTableObject).executeBlocking();

// Non-blocking
operator.insert(myTableObject)
    .execute()
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

Fetch by id and update:
---
```java
final MyTable fetchedObject = operator.getSingle(1).executeBlocking();
fetchedObject.name = "anotherName";
operator.update(fetchedObject).executeBlocking();
```

Fetch by query:
---
```java
final List<MyTable> list = operator
    .getList()
    .withQuery(
        SQLiteQuery
            .builder()
            .where("`id` = ?", 1)
            .build()
    ).executeBlocking();
```

Fetch by raw query and delete:
---
```java
final List<MyTable> list = operator
    .getList()
    .withRawQuery("SELECT * FROM myTable where `id` = ?", 1)
    .executeBlocking();

operator.delete(list).executeBlocking();

```

Delete by query:
---
```java
operator
    .delete()
    .withQuery(
        SQLiteQuery
            .builder()
            .where("`id` = ?", 1)
            .build()
    ).executeBlocking();
```
