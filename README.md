[![Build Status](https://travis-ci.org/jeppeman/HighLite.svg?branch=master)](https://travis-ci.org/jeppeman/HighLite)

HighLite
===

HighLite is an SQLite library for Android that makes use of annotation processing to generate boilerplate for your SQLite operations.

<b>Key features:</b>

* Automated table creation and table upgrades (column additions / changes / deletions are automatic) with opt-out possibilities for those who do not want it. 
* Query builder that eliminates the need to have to deal with the null-argument passing to the standard Android SQLite API.
* Easy to use API with simple but flexible operations for get, save and delete.
* Reactive! Each operation can be Rx-ified for those who use RxJava.
* Supports inheritance of database models
* Annotation driven design, which includes support for foreign keys and relationships.
* Support for the rest of the column constraints available for SQLite, i.e. UNIQUE, NOT NULL and AUTOINCREMENT

<b>Other positives:</b>

* Fast! No reflection resolving at runtime, all operations are carried out through compile time generated code
* Errors in user setup are caught and reported at compile time
* Lint warnings for errors that can't be caught at compile time
* Comprehensive test coverage
* Type safe operations
* No need to subclass SQLiteOpenHelper; all necessary interactions with it are done under the hood.

Getting started
---
```groovy
dependencies {
    compile 'com.jeppeman:highlite:1.0-beta1'
    annotationProcessor 'com.jeppeman:highlite-compiler:1.0-beta1'
}

```

Kotlin users will have to replace `annotationProcessor` with `kapt`.

Basic setup
---
Annotate a class with ```@SQLiteDatabaseDescriptor``` as follows:
```java

@SQLiteDatabaseDescriptor(
    dbName = "companyDatabase",
    dbVersion = 1 // Increment this to trigger an upgrade
)
public class CompanyDatabase {

    // Optional: define a method like this if you want to manually handle onOpen.
    // Note: PRAGMA foreign_keys = ON is set automatically if any foreign
    // keys are found for any table in the database.
    @OnOpen
    public static void onOpen(SQLiteDatabase db) {
        ...
    }
    
    // Optional: define a method like this if you want to manually handle onCreate;
    // i.e. if you opt out from automatic table creation on some table.
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

Then define a class for a table that links to the database class

```java
@SQLiteTable(
        database = CompanyDatabase.class, 
        tableName = "companies", // If left empty, the name of the table defaults to the class name snake cased
        autoCreate = true, // defaults to true, set to false if you do not want the table to be created automatically
        autoAddColumns = true, // defaults to true, set to false if you do not want new columns to be added automatically on upgrades
        autoDeleteColumns = false // defaults to false, set to true if you want deleted fields to be removed from the database automatically on upgrades
)
public class Company {
    
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteColumn need to be at least package local
    
    @SQLiteColumn("companyName") // Column name becomes companyName here
    String name;
    
    @SQLiteColumn
    Date created; // Dates are stored as INTEGER's with the amount of seconds since UNIX epoch
    
    @SQLiteColumn
    List<String> employees; // Fields with types that cannot be matched against an SQLite data type will be serialized and stored as BLOB's 
}
```

That's it, you're now ready to start doing some actual database operations.

<b>Note to Kotlin users</b>


For now, Kotlin properties has to be annotated with `@JvmField` as follows:

```kotlin
@SQLiteTable(database = CompanyDatabase::class)
class Company {
    
    @JvmField
    @SQLiteColumn(primaryKey = PrimaryKey(autoIncrement = true))
    var id : Int = 0
    
    @JvmField
    @SQLiteColumn("companyName")
    var name : String = ""
}
```

This is because a Kotlin property by default is compiled to a private Java field with a getter 
and a setter method. I will address this soon.

Operations
---

### Insert an object

```java
SQLiteOperator<Company> operator = SQLiteOperator.from(getContext(), Company.class);
final Company companyObject = new Company();
companyObject.name = "My awesome company";
companyObject.created = new Date();
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

### Fetch by id and update

```java
// If you pass an argument to getSingle it will be matched against the table's primary key field,
// in this case `id` = 1
final Company fetchedObject = operator.getSingle(1).executeBlocking();
fetchedObject.name = "My not so awesome company";
operator.save(fetchedObject).executeBlocking();
```

### Fetch by query

```java
final List<Company> list = operator
    .getList()
    .withQuery(
        SQLiteQuery
            .builder()
            .where("`id` = ? AND `companyName` = ?", 1, "My not so awesome company")
            .build()
    ).executeBlocking();
```

### Fetch by raw query and delete

```java
final List<Company> list = operator
    .getList()
    .withRawQuery("SELECT * FROM companies where `id` = ?", 1)
    .executeBlocking();

operator.delete(list).executeBlocking();

```

### Delete by query

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
---

HighLite supports foreign keys and relationships, here's an example of how you can use them:

```java
@SQLiteTable(
        database = CompanyDatabase.class, 
        tableName = "companies"
)
public class Company {
    
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id;
    
    @SQLiteColumn("companyName")
    String name;
    
    @SQLiteRelationship(table = Employee.class, backReference = "company") // backReference needs to be the name of the foreign key field of the class it is referring to
    List<Employee> employeeList; // When a company is fetched from the database, its related employees gets fetched as well
}

@SQLiteTable(
        database = CompanyDatabase.class, 
        tableName = "employees"
)
public class Employee {
    
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id; // fields annotated with @SQLiteColumn need to be package local
    
    @SQLiteColumn("employeeName")
    String name;
    
    @SQLiteColumn
    float salary;
    
    @SQLiteColumn(foreignKey = @ForeignKey(
          fieldReference = "id", // Note: this is the name of the field of the class you are referring to, not the database column name; the field has to be unique
          cascadeOnDelete = true, // defaults to false
          cascadeOnUpdate = true // defaults to false
    ))
    Company company; // When an employee is fetched, this field is automatically instantiated as its corresponding Company
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
john.company = company;
bob.name = "Bob";
bob.salary = 10000f;
bob.company = company;
employeeOperator.save(john, bob).executeBlocking();
```

Now if we fetch the company from the database the employees will follow:
```java
Company companyFromDatabase = companyOperator
    .getSingle()
    .withRawQuery("SELECT * FROM companies WHERE `name` = ?", "My awesome company")
    .executeBlocking();
    
Log.d("employees", companyFromDatabase.employeeList /* <- this is now [john, bob]*/)
```

Inheritance
-----------

HighLite supports inheritance of classes annotated with `SQLiteTable`, consider the following:

```java
@SQLiteTable(
        database = CompanyDatabase.class
)
public class Developer extends Employee {

    @SQLiteColumn
    String type;
}
```

Here the class `Developer` extends `Employee`, which is already annotated with `SQLiteTable`, the 
create statement that is generated from this setup looks like this:

```roomsql
CREATE TABLE IF NOT EXISTS developer (
    `type` TEXT,
    `employees_ptr_id` INTEGER PRIMARY KEY NOT NULL,
    FOREIGN KEY(`employees_ptr_id`) REFERENCES employees(`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
```

So we have a one-to-one relationship between `Developer` and `Employee`, therefore the primary key
for `Developer` is automatically created as a pointer to the primary key of `Employee`.

Let's illustrate what happens when we use operations on the `Developer` class.

```java
SQLiteOperator operator = SQLiteOperator.from(getContext(), Developer.class);
Developer dev = new Developer();
dev.name = "Bob";
dev.salary = 10000f;
dev.company = company;
dev.type = "Android";

// When we save the object, the values of the fields are saved to the table they correspond to in
// the class hierarchy; in this case, name, salary and company are saved to the employees table,
// whereas type is saved to the developer table
operator.save(dev).executeBlocking();

// Now if we fetch all developers from the database, a JOIN will be performed on the developer and
// employees tables and fields will be populated accordingly.
List<Developer> devsFromDb = operator.getList().executeBlocking();
```

You may also want to inherit from a base class that is not corresponding to a table in the database,
in that case, the following works:

```java
public class TimestampedModel {

    @SQLiteColumn
    Date created;
    
    @SQLiteColumn
    Date modified;
}

@SQLiteTable(
        database = CompanyDatabase.class, 
        tableName = "companies"
)
public class Company extends TimestampedModel {
    
    @SQLiteColumn(primaryKey = @PrimaryKey(autoIncrement = true))
    long id;
    
    @SQLiteColumn("companyName")
    String name;
    
    @SQLiteRelationship(table = Employee.class, backReference = "company") // backReference needs to be the name of the foreign key field of the class it is referring to
    List<Employee> employeeList; // When a company is fetched from the database, its related employees gets fetched as well
}
```

With this setup, the following create statement is generated:

```roomsql
CREATE TABLE IF NOT EXISTS companies (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `companyName` TEXT,
    `created` INTEGER,
    `employees` BLOB,
    `created` INTEGER,
    `modified` INTEGER
);
```

Upcoming features
-----------

* More flexibility when it comes to the migrations
* Composite primary key support
* Kotlin improvements




