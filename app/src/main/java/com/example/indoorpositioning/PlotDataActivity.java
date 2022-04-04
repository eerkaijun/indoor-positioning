package com.example.indoorpositioning;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class PlotDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot_data);
    }

    public void plotMeasurement(View view) {
        Intent intent = new Intent(this, PlotMeasurementActivity.class);
        startActivity(intent);
    }

    public void plotPDR(View view) {
        Intent intent = new Intent(this, PlotPDRActivity.class);
        startActivity(intent);
    }

}
