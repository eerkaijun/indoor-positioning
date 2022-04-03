//By Salman Saiful Redzuan
/*
This class prompts the users to give their height and gender to estimate the step length.
 */

package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class UserPromptActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner genderSpinner;

    static int gender;
    static float height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_prompt);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        genderSpinner = (Spinner) findViewById(R.id.spinner);
        genderSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.genderOption, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        genderSpinner.setAdapter(adapter);

        return true;
    }

    public void openMap (View view) {
        Intent intent = new Intent(this, MapActivity.class);

        //get data for height
        EditText editText = findViewById(R.id.editTextNumber);
        height = Float.valueOf(editText.getText().toString());

        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
            case 1:
                gender = 1;
                break;
            case 2:
                gender = 0;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}