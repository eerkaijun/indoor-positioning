package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

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
        try {
            saveRecording();
        } catch(Exception ex) {
            Log.e("Error saving into csv file", ex);
        } finally {
            // then clear the buffer
            sensorBuffer = new ArrayList<ArrayList<Float>>();
        }
    }

    private void saveRecording() throws IOException {
        // Create an csv file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFIleName = "Sensor_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File data = File.createTempFile(
                imageFIleName,
                ".csv",
                storageDir
        );

        // write sensor data to file
        FileWriter fw = new FileWriter(data);
        BufferedWriter bw = new BufferedWriter(fw);
        for (int i=0; i<sensorBuffer.size(); i++) {
            bw.write(sensorBuffer.get(i).get(0)+","+sensorBuffer.get(i).get(1)+","+sensorBuffer.get(i).get(2));
            bw.newLine();
        }
        bw.close();
        fw.close();
    }
}