package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class RecordDataActivity extends AppCompatActivity implements SensorEventListener {

    static final int REQUEST_ID_READ_WRITE_PERMISSION = 0;

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

        askPermission();

        // initialise sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticField = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
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
        Log.d("Buffer content", sensorBuffer.toString());
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
            Log.e("Error saving into csv file", ex.toString());
        } finally {
            // then clear the buffer
            sensorBuffer = new ArrayList<ArrayList<Float>>();
        }
    }

    private void saveRecording() throws IOException {
        // Create an csv file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFIleName = "Sensor_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File data = File.createTempFile(
                imageFIleName,
                ".csv",
                storageDir
        );
        Log.d("File path", data.getAbsolutePath());

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

    private void askPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int readPermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_ID_READ_WRITE_PERMISSION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ID_READ_WRITE_PERMISSION: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
}