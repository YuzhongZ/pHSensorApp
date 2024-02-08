package com.example.phsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private Button mBtnLogin;
    private EditText mEtUser;
    private EditText mEtPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnLogin = findViewById(R.id.btn_login);
        mEtUser = findViewById(R.id.et_1);
        mEtPassword = findViewById(R.id.et_2);


        /*mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = null;
                intent = new Intent(MainActivity.this, FunctionActivity.class);
                startActivity(intent);
            }
        });*/

        mBtnLogin.setOnClickListener(this::onClick);


    }


    private void onClick(View v){
        String username = mEtUser.getText().toString();
        String password = mEtPassword.getText().toString();
        Intent intent = null;
        if (username.equals("yz") && password.equals("123456")) {
            intent = new Intent(MainActivity.this, FunctionActivity.class);
            startActivity(intent);
        }
        else {

        }



    }
}