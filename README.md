[![Build Status](https://travis-ci.org/jeppeman/LiteOmatic.svg?branch=master)](https://travis-ci.org/jeppeman/LiteOmatic)

LiteOmatic
===

LiteOmatic is an SQLite library for Android that makes use of annotation processing to generate boilerplate for your SQLite operations.

<b>Key features:</b>

* No need to define subclasses of SQLiteOpenHelper; automates table creation, table deletion and table upgrades
* Query builder that eliminates the need to have to deal with the null-argument passing to the standard Android SQLite API.
* Easy to use API with blocking and non-blocking (using RxJava) operations for get, save and delete.
* Annotation driven design, which includes support for foreign keys and relationships

<b>Other positives:</b>

* Fast! No reflection resolving at runtime, all operations are carried out through compile time generated code
* Errors in user setup are caught and reported at compile time
* Lint warnings for errors that can't be caught at compile time
* Comprehensive test coverage
* Type safe operations

Getting started
---
```groovy
dependencies {
    compile 'com.jeppeman:liteomatic:1.0-beta1'
    annotationProcessor 'com.jeppeman:liteomatic-compiler:1.0-beta1'
}

```

Basic setup
---
Annotate a class with ```@SQLiteDatabaseDescriptor``` as follows:
```java

@SQLiteDatabaseDescriptor(
    dbName = "myDatabase",
    dbVersion = 1 // Increment this to trigger an upgrade
)
public class MyDatabase {

    // Optional: define a method like this if you want to manually handle onOpen.
    // Note: PRAGMA foreign_keys = ON is set automatically if any foreign
    // keys are found for any table in the database.
    @OnOpen
    public static void onOpen(SQLiteDatabase db) {
        ...
    }
    
    // Optional: define a method like this if you want to manually handle onCreate;
    // i.e. if you opt out from automatic table creation on some table
    @OnCreate
    public static void onCreate(SQLiteDatabase db) {
        ...
    }
    
    // Optional: define a method like this if you want to manually handle onUpgrade;
    // i.e. if you opt out from automatic upgrades on some table 
    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ...
    }
    
}
```

Then define a class for a table

```java
@SQLiteTable(
        database = MyDatabase.class, 
        tableName = "companies",
        autoCreate = true, // defaults to true, set to false if you do not want the table to be created automatically
        autoAddColumns = true, // defaults to true, set to false if you do not want new columns to be added automatically on upgrades
        autoDeleteColumns = false // defaults to false, set to true if you want deleted fields to be removed from the database automatically on upgrades
)
public class Company {
    
    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteField need to be at least package local
    
    @SQLiteField(fieldName = "companyName")
    String name;
    
    @SQLiteField
    List<String> employees; // This will get saved as a BLOB in the database
}
```

That's it, you're now ready to start doing some actual database operations

Operations
===
The operations

Insert an object
---
```java
SQLiteOperator<Company> operator = SQLiteOperator.from(getContext(), Company.class);
final Company companyObject = new Company();
companyObject.name = "My awesome company";
companyObject.employees = Arrays.asList("John", "Bob");

// Blocking
operator.save(companyObject).executeBlocking(); // the save method inserts if the object's id is not present in the table, otherwise updates

// Non-blocking
operator.save(companyObject)
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

Fetch by id and update
---
```java
// If you pass an argument to getSingle it will be matched against the table's primary key field,
// in this case `id` = 1
final Company fetchedObject = operator.getSingle(1).executeBlocking();
fetchedObject.name = "Mary";
operator.save(fetchedObject).executeBlocking();
```

Fetch by query
---
```java
final List<Company> list = operator
    .getList()
    .withQuery(
        SQLiteQuery
            .builder()
            .where("`id` = ?", 1)
            .build()
    ).executeBlocking();
```

Fetch by raw query and delete
---
```java
final List<Company> list = operator
    .getList()
    .withRawQuery("SELECT * FROM companies where `id` = ?", 1)
    .executeBlocking();

operator.delete(list).executeBlocking();

```

Delete by query
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

Foreign keys and relationships
===

LiteOmatic supports foreign keys and relationships, here's an example of how you can use them:

```java
@SQLiteTable(
        database = MyDatabase.class, 
        tableName = "companies"
)
public class Company {
    
    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long id;
    
    @SQLiteField(fieldName = "companyName")
    String name;
    
    @SQLiteRelationship(table = Employee.class)
    List<Employee> employeeList; // When a company is fetched from the database, its related employees gets fetched as well
}

@SQLiteTable(
        database = MyDatabase.class, 
        tableName = "employees"
)
public class Employee {
    
    @SQLiteField(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteField need to be package local
    
    @SQLiteField(fieldName = "employeeName")
    String name;
    
    @SQLiteField
    float salary;
    
    @SQLiteField(foreignKey = @ForeignKey(
          table = Company.class,
          fieldReference = "id", // Note: this is the name of the field of the class you are referring to, not the database column name; the field has to be unique
          cascadeOnDelete = true, // defaults to false
          cascadeOnUpdate = true // defaults to false
    ))
    long companyId;
}
```

Let's create a company with a couple of employees:

```java
SQLiteOperator<Company> companyOperator = SQLiteOperator.from(getContext(), Company.class);
Company company = new Company();
company.name = "My awesome company";
companyOperator.save(company).executeBlocking();

SQLiteOperator<Employee> employeeOperator = SQLiteOperator.from(getContext(), Employee.class);
Employee john = new Employee(),
    bob = new Employee();
john.name = "John";
john.salary = 1000f;
john.companyId = company.id;
bob.name = "Bob";
bob.salary = 10000f;
bob.companyId = company.id;
employeeOperator.save(john, bob).executeBlocking();
```

Now if we fetch the commpany from the database the employees will follow:
```java
Company companyFromDatabase = companyOperator
    .getSingle()
    .withRawQuery("SELECT * FROM companies WHERE `name` = ?", "My awesome company")
    .executeBlocking();
    
Log.d("employees", companyFromDatabase.employeeList /* <- this is now [john, bob]*/)
```