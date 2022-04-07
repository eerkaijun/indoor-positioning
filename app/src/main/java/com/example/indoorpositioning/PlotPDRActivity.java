//By Petros Koutsouvelis
/*
This activity plots a PDR trajectory and display it on the user interface.
 */

package com.example.indoorpositioning;

import android.content.Intent;
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

public class PlotPDRActivity extends AppCompatActivity {

    //Declare variables
    EditText et2, et3;
    Button btn2;
    ImageView iv2;
    String Sex;
    String Height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot_pdr);

        et2 = (EditText) findViewById(R.id.insert_num2);
        et3 = (EditText) findViewById(R.id.insert_num3);
        btn2 = (Button) findViewById(R.id.plot_btn2);
        iv2 = (ImageView) findViewById(R.id.plot_sensor_image2);

        // Start Python if not already started and create python instance
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        final Python py = Python.getInstance();

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Read user's input
                Sex = et2.getText().toString();
                Height = et3.getText().toString();

                //Create python object to load script
                PyObject pyobj = py.getModule("pdr_1");
                PyObject obj = pyobj.callAttr("pdr", Sex, Height);

                //Create a string
                String str = obj.toString();
                //Convert it to byte array
                byte data[] = android.util.Base64.decode(str, Base64.DEFAULT);
                //Convert to bitmap
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                //Set it to image view
                iv2.setImageBitmap(bmp);
            }
        });
    }

    public void align(View view) {
        Intent intent = new Intent(this, AlignOptionsActivity.class);
        intent.putExtra("SEX", Sex);
        intent.putExtra("HEIGHT", Height);
        startActivity(intent);
    }

}
