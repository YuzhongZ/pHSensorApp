package com.example.phsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FunctionActivity extends AppCompatActivity {

    private Button mBtnBLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);

        mBtnBLE = findViewById(R.id.btn_func_ble);
        mBtnBLE.setOnClickListener(this::onClick);



    }

    private void onClick(View v){

        Intent intent = null;
        intent = new Intent(FunctionActivity.this, BLEActivity.class);
        startActivity(intent);

        }
}