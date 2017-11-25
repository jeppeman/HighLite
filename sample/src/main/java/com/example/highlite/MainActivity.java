package com.example.highlite;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jeppeman.highlite.SQLiteOperator;

import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SQLiteOperator<Employee> operator = SQLiteOperator.from(this,
                Employee.class);

        operator
                .getList()
                .execute()
                .subscribe(new Subscriber<Employee>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Employee myLittleClass2) {

                    }
                });

//        Company m = new Company();
//        m.name = "asdf";
//        m.nameList = Arrays.asList("a, ", "c", "b");
//        m.foreign = 7;
//        SQLiteOperator.from(this, Company.class)
//                .insert(m)
//                .execute()
//                .subscribe(new Completable.CompletableSubscriber() {
//                    @Override
//                    public void onCompleted() {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onSubscribe(Subscription d) {
//
//                    }
//                });
    }
}