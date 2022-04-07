package com.example.indoorpositioning;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class AlignOptionsActivity  extends AppCompatActivity {
    //Declare variables
    EditText et1;
    TextView tv1, tv2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_align_options);

        Intent intent = getIntent();
        String sex = intent.getStringExtra("SEX");
        String height = intent.getStringExtra("HEIGHT");

        et1 = (EditText) findViewById(R.id.startposition);
        tv1 = (TextView) findViewById(R.id.heightread);
        tv2 = (TextView) findViewById(R.id.sexread);
        tv1.setText(height);
        tv2.setText(sex);
    }


    public void align1(View view) {
        String Sex = tv2.getText().toString();
        String Height = tv1.getText().toString();
        Intent intent = new Intent(this, AlignPDRActivity.class);
        intent.putExtra("SEX", Sex);
        intent.putExtra("HEIGHT", Height);
        startActivity(intent);
    }

    public void align2(View view) {
        String Sex = tv2.getText().toString();
        String Height = tv1.getText().toString();
        Intent intent = new Intent(this, AutomaticAlignActivity.class);
        intent.putExtra("SEX", Sex);
        intent.putExtra("HEIGHT", Height);
        startActivity(intent);
    }
}
