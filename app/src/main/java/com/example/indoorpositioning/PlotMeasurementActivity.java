//By Petros Koutsouvelis
/*
This activity plots the sensory data and display it on the user interface.
 */

package com.example.indoorpositioning;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class PlotMeasurementActivity extends AppCompatActivity {

    //Declare variables
    EditText et1;
    Button btn1;
    ImageView iv1;
    String Sensor_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot_measurement);

        et1 = (EditText) findViewById(R.id.insert_num);
        btn1 = (Button) findViewById(R.id.plot_btn);
        iv1 = (ImageView) findViewById(R.id.plot_sensor_image);

        // Start Python if not already started and create python instance
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        final Python py = Python.getInstance();

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Read user's input
                Sensor_num = et1.getText().toString();

                //Create python object to load script
                PyObject pyobj = py.getModule("sensor_plot");
                PyObject obj = pyobj.callAttr("sensor_plot", Sensor_num);

                //Create a string
                String str = obj.toString();
                //Convert it to byte array
                byte data[] = android.util.Base64.decode(str, Base64.DEFAULT);
                //Convert to bitmap
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                //Set it to image view
                iv1.setImageBitmap(bmp);
            }
        });

    }

}
