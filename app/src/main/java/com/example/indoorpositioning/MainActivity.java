package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void recordData(View view) {
        Intent intent = new Intent(this, RecordDataActivity.class);
        startActivity(intent);
    }

    public void openPrompt(View view) {
        Intent intent = new Intent(this, UserPromptActivity.class);
        startActivity(intent);
    }
}