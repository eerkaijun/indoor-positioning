//By Kai Jun Eer
/*
This activity implements a data recording module that collects magnetometer, accelerometer and wifi.
We use the accelerometer and magnetometer data to calculate the orientation of the device. At the end
of data recording, these data is saved into a csv file.
 */

package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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
import java.util.Iterator;
import java.util.List;

public class RecordDataActivity extends AppCompatActivity implements SensorEventListener {

    // get access to sensors
    private SensorManager sensorManager;

    // get access to wifi
    private WifiManager wifiManager;

    // represent a sensor
    private Sensor magneticField;
    private Sensor accelerometer;
    private Sensor gyroscope;

    // state to keep track of whether recording is in progress
    boolean recordingInProgress = false;

    // Sensors data value used to calculate orientation
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    // sensors buffers during recording
    ArrayList<ArrayList<Float>> sensorBuffer = new ArrayList();
    ArrayList<String> wifiBuffer = new ArrayList();
    // buffer to store all the sensor and wifi data at one time instance
    ArrayList<Float> singlePointBuffer = new ArrayList(
            Arrays.asList(0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0)
    );
    String wifiSinglePointBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_data);

        // initialise sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticField = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // initialise wifi
        wifiManager =  (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // SENSOR_DELAY_NORMAL approximately 0.2s
        // SENSOR_DELAY_GAME approximately 0.02s
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        // register wifi
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        unregisterReceiver(wifiScanReceiver);
    }

    private float calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        // calculate degrees
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        float degree = (float) Math.toDegrees(values[0]);

        return degree;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            double h = Math.sqrt(sensorEvent.values[0] * sensorEvent.values[0] + sensorEvent.values[1] * sensorEvent.values[1] + sensorEvent.values[2] * sensorEvent.values[2]);
            // each datapoint is a four dimensional magnetic field sensor data
            singlePointBuffer.set(0, sensorEvent.values[0]);
            singlePointBuffer.set(1, sensorEvent.values[1]);
            singlePointBuffer.set(2, sensorEvent.values[2]);
            singlePointBuffer.set(3, (float) h);

            // for degree calculation
            magneticFieldValues[0] = sensorEvent.values[0];
            magneticFieldValues[1] = sensorEvent.values[1];
            magneticFieldValues[2] = sensorEvent.values[2];

            // keep orientation degree in buffer
            float degree = calculateOrientation();
            singlePointBuffer.set(10, degree);

            if (recordingInProgress) {
                // deep copy
                ArrayList<Float> singlePointBufferClone = new ArrayList<>();
                Iterator<Float> it = singlePointBuffer.iterator();
                while (it.hasNext()) {
                    Float x = it.next();
                    singlePointBufferClone.add(x);
                }
                sensorBuffer.add(singlePointBufferClone);

                // sync wifi manager
                wifiBuffer.add(wifiSinglePointBuffer);
                //registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                wifiManager.startScan();
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // each datapoint is a three dimensional accelerometer sensor data
            singlePointBuffer.set(4, sensorEvent.values[0]);
            singlePointBuffer.set(5, sensorEvent.values[1]);
            singlePointBuffer.set(6, sensorEvent.values[2]);

            // for degree calculation
            accelerometerValues[0] = sensorEvent.values[0];
            accelerometerValues[1] = sensorEvent.values[1];
            accelerometerValues[2] = sensorEvent.values[2];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // each datapoint is a three dimensional gyroscope sensor data
            singlePointBuffer.set(7, sensorEvent.values[0]);
            singlePointBuffer.set(8, sensorEvent.values[1]);
            singlePointBuffer.set(9, sensorEvent.values[2]);
        }
        Log.d("Single buffer content", singlePointBuffer.toString());
        Log.d("Overall buffer content", sensorBuffer.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void startRecording(View view) {
        recordingInProgress = true;
        Toast.makeText(this, "Recording started!", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Recording stopped!", Toast.LENGTH_LONG).show();
        }

    }

    // called when the user would like to view plots of previously recorded data
    public void goPlotsOptionPage(View view) {
        if(!recordingInProgress) {
            Intent intent = new Intent(this, PlotDataActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "Stop the recording to plot results!", Toast.LENGTH_LONG).show();
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
        bw.write("mag_x,mag_y,mag_z,mag_h,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,degree,wifi"); // header for csv file
        bw.newLine();
        for (int i=0; i<sensorBuffer.size(); i++) {
            bw.write(sensorBuffer.get(i).get(0)+","+
                    sensorBuffer.get(i).get(1)+","+
                    sensorBuffer.get(i).get(2)+","+
                    sensorBuffer.get(i).get(3)+","+
                    sensorBuffer.get(i).get(4)+","+
                    sensorBuffer.get(i).get(5)+","+
                    sensorBuffer.get(i).get(6)+","+
                    sensorBuffer.get(i).get(7)+","+
                    sensorBuffer.get(i).get(8)+","+
                    sensorBuffer.get(i).get(9)+","+
                    sensorBuffer.get(i).get(10)+","+
                    wifiBuffer.get(i));
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            //unregisterReceiver(this);

            wifiSinglePointBuffer = "";
            for (int i=0; i<wifiScanList.size(); i++) {
                wifiSinglePointBuffer = wifiSinglePointBuffer + wifiScanList.get(i).SSID +
                        '|' + wifiScanList.get(i).BSSID +
                        '|' + String.valueOf(wifiScanList.get(i).level) + ';';
            }
            Log.d("WiFi output", wifiSinglePointBuffer);
        }
    };
}