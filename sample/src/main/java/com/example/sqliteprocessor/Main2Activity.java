package com.example.sqliteprocessor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jeppeman.sqliteprocessor.SQLiteOperator;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        MyLittleClass2 table = new MyLittleClass2();
        table.namezz = "xxx";
        SQLiteOperator.from(this, MyLittleClass2.class)
                .insert(table)
                .executeBlocking();



//        MyLittleClass m = new MyLittleClass();
//        m.name = "asdf";
//        m.nameList = Arrays.asList("a, ", "c", "b");
//        m.foreign = 7;
//        SQLiteOperator.from(this, MyLittleClass.class)
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