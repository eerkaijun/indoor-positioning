//By Salman Saiful Redzuan
/*
This class is where all the data processing happens. It receives a signal from sensors on the phone and translates it into steps and heading angle.
 */

package com.example.indoorpositioning;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Map;

public class MovementDetection implements SensorEventListener {
    //listener that pass through info of the activities
    private OnMovementDetectionManagerListener movementDetectionManagerListener;

    // get access to sensors
    private SensorManager sensorManager;

    // represent a sensor
    private Sensor magneticField;
    private Sensor accelerometer;

    // initialise parameters
    private boolean stepDetect = false;
    private boolean stepCounted = true;
    ////wait for sensor to stabilised before counting the steps
    private int steps = -1;
    ////threshold for steps
    final private float uThreshold = 0.108f;
    final private float lThreshold = 0.088f;

    // Sensors data value used to calculate orientation
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    //constructor
    public MovementDetection(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    //listener
    public void setOnMovementDetectionManagerListener(OnMovementDetectionManagerListener movementDetectionManagerListener) {
        this.movementDetectionManagerListener = movementDetectionManagerListener;
    }

    //register sensor
    public void registerSensors(){
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //unregister sensor
    public void unregisterSensors(){
        sensorManager.unregisterListener(this);
    }

    //Listeners for activities when data changed
    public interface OnMovementDetectionManagerListener {
        void onHeadingUpdated(float degree);
        void onStepDetected(float lat, float lon);
    }

    private double magnitude;
    private float[] hpfiltered = new float[3];
    private float[] lpfiltered = new float[3];
    private float[] grav = new float[3];

    //coordinate initialise
    float prev_x = 0.0f;
    float prev_y = 0.0f;
    float x, y;

    //world coordinate initialise
    float prev_lat = (float)MapActivity.currLocation.latitude;
    float prev_lon = (float)MapActivity.currLocation.longitude;
    float lat, lon;
    final float lat_deg_per_m = (float) (1/110947.2);
    final float lon_deg_per_m = (float) (1/87843.36);

    //angle parameters
    final float min_degree = 10.0f;
    final float max_degree = 80.0f;
    float prev_degree = 0.0f;
    float degree = 0.0f;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            hpfiltered = highPass(sensorEvent.values);
            lpfiltered = lowPass(hpfiltered);
            magnitude = Math.sqrt((lpfiltered[0] * lpfiltered[0]) + (lpfiltered[1] * lpfiltered[1]) + (lpfiltered[2] * lpfiltered[2]));

            // for degree calculation
            accelerometerValues[0] = sensorEvent.values[0];
            accelerometerValues[1] = sensorEvent.values[1];
            accelerometerValues[2] = sensorEvent.values[2];
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // for degree calculation
            magneticFieldValues[0] = sensorEvent.values[0];
            magneticFieldValues[1] = sensorEvent.values[1];
            magneticFieldValues[2] = sensorEvent.values[2];
        }

        //step length
        float sl = stepLength();

        //heading angle
        degree = calculateOrientation();
        degree = lowPass(degree);
        movementDetectionManagerListener.onHeadingUpdated(degree);

        //step detector
        if (!stepDetect) {
            if (magnitude > uThreshold) {
                stepDetect = true;
                stepCounted = false;
            }
        }
        else if (magnitude < lThreshold) {
            stepDetect = false;
        }

        if (stepDetect && !stepCounted) {
            stepCounted = true;
            steps = steps + 1;

            if (steps > 0) {
                //check if the change of the angle is more than 10 degrees
                if (Math.abs(prev_degree - degree) <= min_degree) {
                    //coordinate
                    x = (float) (prev_x + (sl * Math.sin(Math.toRadians(prev_degree))));
                    y = (float) (prev_y + (sl * Math.cos(Math.toRadians(prev_degree))));

                    //world coordinate
                    lat = (float) (prev_lat + ((sl * Math.cos(Math.toRadians(prev_degree))) * lat_deg_per_m));
                    lon = (float) (prev_lon + ((sl * Math.sin(Math.toRadians(prev_degree))) * lon_deg_per_m));
                }
                //if the angle is between the min and max threshold
                else if (Math.abs(prev_degree - degree) > min_degree &&
                        Math.abs(prev_degree - degree) < max_degree) {
                    //coordinate
                    x = (float) (prev_x + (sl * Math.sin(Math.toRadians(degree))));
                    y = (float) (prev_y + (sl * Math.cos(Math.toRadians(degree))));

                    //world coordinate
                    lat = (float) (prev_lat + ((sl * Math.cos(Math.toRadians(degree))) * lat_deg_per_m));
                    lon = (float) (prev_lon + ((sl * Math.sin(Math.toRadians(degree))) * lon_deg_per_m));
                }
                //more than 80 degree change, a turn detected
                else {
                    //a right turn detected
                    if ((degree - prev_degree) > 0) {
                        //coordinate
                        x = (float) (prev_x + (sl * Math.sin(Math.toRadians(prev_degree + 90))));
                        y = (float) (prev_y + (sl * Math.cos(Math.toRadians(prev_degree + 90))));

                        //world coordinate
                        lat = (float) (prev_lat + ((sl * Math.cos(Math.toRadians(prev_degree + 90))) * lat_deg_per_m));
                        lon = (float) (prev_lon + ((sl * Math.sin(Math.toRadians(prev_degree + 90))) * lon_deg_per_m));

                        prev_degree = prev_degree + 90;
                    }
                    //a left turn detected
                    else {
                        //coordinate
                        x = (float) (prev_x + (sl * Math.sin(Math.toRadians(prev_degree - 90))));
                        y = (float) (prev_y + (sl * Math.cos(Math.toRadians(prev_degree - 90))));

                        //world coordinate
                        lat = (float) (prev_lat + ((sl * Math.cos(Math.toRadians(prev_degree - 90))) * lat_deg_per_m));
                        lon = (float) (prev_lon + ((sl * Math.sin(Math.toRadians(prev_degree - 90))) * lon_deg_per_m));

                        prev_degree = prev_degree - 90;
                    }
                }
                movementDetectionManagerListener.onStepDetected(lat, lon);

                //set coordinate
                prev_x = x;
                prev_y = y;

                //set world coordinate
                prev_lat = lat;
                prev_lon = lon;
            }
        }
    }

    //step length calculator
    private float stepLength () {
        float h;

        //default height
        if (UserPromptActivity.height == 0) {
            h = 1.65f;
        }
        else {
            h = UserPromptActivity.height;
        }

        //default gender -- male
        if (UserPromptActivity.gender == 1) {
            return (0.415f * h);
        }
        //female
        else {
            return (0.413f * h);
        }
    }

    //high pass filter for getting rid of gravity
    private float[] highPass(float[] mag) {
        float alpha = 0.2f;
        float[] filtered = new float[3];

        grav[0] = (alpha * grav[0]) + ((1-alpha) * mag[0]);
        grav[1] = (alpha * grav[1]) + ((1-alpha) * mag[1]);
        grav[2] = (alpha * grav[2]) + ((1-alpha) * mag[2]);

        //mean_grav = (mag * (1-alpha)) + (mean_grav*alpha);

        filtered[0] = mag[0] - grav[0];
        filtered[1] = mag[1] - grav[1];
        filtered[2] = mag[2] - grav[2];

        return filtered;
    }

    //low pass filter for accelerometer smoothing
    private float[] lowPass(float[] mag) {
        float alpha = 0.2f;
        float[] filtered = new float[3];

        filtered[0] = (mag[0] * alpha) + (filtered[0] * (1-alpha));
        filtered[1] = (mag[1] * alpha) + (filtered[1] * (1-alpha));
        filtered[2] = (mag[2] * alpha) + (filtered[2] * (1-alpha));
        return filtered;
    }

    //low pass filter for angle smoothing
    float filteredangle = 0.0f;
    private float lowPass(float mag) {
        float alpha = 0.2f;

        filteredangle = (mag * alpha) + (filteredangle * (1-alpha));
        return filteredangle;
    }

    //heading angle calculator
    private float calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        // calculate degrees
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        return (float) Math.toDegrees(values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
