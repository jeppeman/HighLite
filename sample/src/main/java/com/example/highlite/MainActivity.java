package com.example.highlite;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jeppeman.highlite.SQLiteOperator;
import com.jeppeman.highlite.SQLiteQuery;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SQLiteOperator<Employee> employeeOperator = SQLiteOperator.from(this,
                Employee.class);
        final SQLiteOperator<Company> companyOperator = SQLiteOperator.from(this,
                Company.class);

        final Company m = new Company();
        m.name = "My awesome company";

        employeeOperator
                .delete()
                .withQuery(SQLiteQuery.builder().where("`id` > ?", 0).build())
                .asSingle()
                .flatMap(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer integer) throws Exception {
                        return companyOperator
                                .delete()
                                .withQuery(
                                        SQLiteQuery.builder().where("`id` > ?", 0).build()
                                ).asSingle();
                    }
                })
                .flatMap(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer integer) throws Exception {
                        return companyOperator
                                .save(m)
                                .asSingle();
                    }
                })
                .flatMap(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer integer) throws Exception {
                        Employee john = new Employee(),
                                bob = new Employee();
                        john.name = "John";
                        john.company = m;
                        bob.name = "Bob";
                        bob.company = m;
                        Log.d(TAG, "Saving emps");
                        return employeeOperator.save(john, bob).asSingle();
                    }
                })
                .flatMapObservable(new Function<Integer, Observable<Employee>>() {
                    @Override
                    public Observable<Employee> apply(Integer integer) throws Exception {
                        List<Employee> l = employeeOperator.getList().executeBlocking();
                        Log.d(TAG, "Fetching emps");
                        return employeeOperator.getList().asObservable();
                    }
                })
                .subscribe(new Consumer<Employee>() {
                    @Override
                    public void accept(Employee employee) throws Exception {
                        Log.d(TAG, "Employee received: " + employee.name);
                    }
                });
    }
}