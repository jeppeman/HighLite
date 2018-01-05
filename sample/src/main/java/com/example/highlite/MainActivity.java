package com.example.highlite;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jeppeman.highlite.SQLiteOperator;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableSingleObserver;

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

        employeeOperator.getSingle(1).execute()
                .subscribe(new DisposableMaybeObserver<Employee>() {

                    @Override
                    public void onSuccess(Employee employee) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Maybe?");
                    }
                });

        companyOperator
                .save(m)
                .execute()
                .flatMap(new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer integer) throws Exception {
                        Employee john = new Employee(),
                                bob = new Employee();
                        john.name = "John";
                        john.company = m;
                        bob.name = "Bob";
                        bob.company = m;
                        return employeeOperator.save(john, bob).execute();
                    }
                })
                .flatMap(new Function<Integer, Single<List<Employee>>>() {
                    @Override
                    public Single<List<Employee>> apply(Integer integer) throws Exception {
                        return employeeOperator.getList().execute();
                    }
                })
                .subscribe(new DisposableSingleObserver<List<Employee>>() {
                    @Override
                    public void onSuccess(List<Employee> employees) {
                        for (Employee employee : employees) {
                            Log.d(TAG, "Employee: " + employee.name);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }
}