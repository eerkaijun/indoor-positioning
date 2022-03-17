package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;

public class RecordDataActivity extends AppCompatActivity implements SensorEventListener {

    // get access to sensors
    private SensorManager sensorManager;

    // represent a sensor
    private Sensor magneticField;

    // state to keep track of whether recording is in progress
    boolean recordingInProgress = false;

    // sensors buffer during recording
    ArrayList<ArrayList<Float>> sensorBuffer = new ArrayList<ArrayList<Float>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_data);

        // initialise sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticField = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double h = Math.sqrt(sensorEvent.values[0] * sensorEvent.values[0] + sensorEvent.values[1] * sensorEvent.values[1] + sensorEvent.values[2] * sensorEvent.values[2]);

        // each datapoint is a four dimensional magnetic field sensor data
        ArrayList<Float> dataPoint = new ArrayList<Float>();
        dataPoint.add(sensorEvent.values[0]);
        dataPoint.add(sensorEvent.values[1]);
        dataPoint.add(sensorEvent.values[2]);
        dataPoint.add((float) h);

        if (recordingInProgress) sensorBuffer.add(dataPoint);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void startRecording(View view) {
        recordingInProgress = true;
    }

    public void stopRecording(View view) {
        recordingInProgress = false;
        // write to csv file
        // then clear the buffer
        sensorBuffer = new ArrayList<ArrayList<Float>>();
    }
}