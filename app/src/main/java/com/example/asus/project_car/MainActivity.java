package com.example.asus.project_car;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button btn1;
    Button btn2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn1=(Button)findViewById(R.id.btn_scan);
        btn1.setOnClickListener(gotoMap);
        btn2=(Button)findViewById(R.id.btn_dis);
        btn2.setOnClickListener(gotoBT);
    }

    View.OnClickListener gotoMap=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent=new Intent();
            intent.setClass(MainActivity.this,MapsActivity.class);
            startActivity(intent);
        }
    };
    View.OnClickListener gotoBT=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent=new Intent();
            intent.setClass(MainActivity.this,BTActivity.class);
            startActivity(intent);
        }
    };
}
