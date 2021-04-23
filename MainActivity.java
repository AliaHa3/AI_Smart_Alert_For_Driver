package com.example.alia.testopencv;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;



public class MainActivity extends AppCompatActivity  {


    Context context;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("openCvTest", "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

    }


    public void startApp(View view) {
        try{
            Intent intent = new Intent(this,FdActivity.class);
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }



}

