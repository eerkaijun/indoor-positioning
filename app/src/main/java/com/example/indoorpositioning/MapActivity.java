//By Salman Saiful Redzuan

package com.example.indoorpositioning;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.SeekBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener {

    //Create a point to initialise where camera points at the beginning
    private static final LatLng KB = new LatLng(55.92257794064054, -3.1718609539182485);

    private static final int TRANSPARENCY_MAX = 100;
    private static final int TRANSPARENCY_DEF = 65;

    private SeekBar transparencyBar;

    private GroundOverlay flFloor0;
    private GroundOverlay hbFloor0;
    private GroundOverlay sandFloor0;

    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup the transparency slider
        transparencyBar = findViewById(R.id.transparencySeekBar);
        transparencyBar.setMax(TRANSPARENCY_MAX);
        transparencyBar.setProgress(TRANSPARENCY_DEF);

        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Get a handle to the GoogleMap object and display marker.
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        //enable indoor view
        map.setIndoorEnabled(true);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);

        //setup the ground overlay
        flFloor0 = map.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.fleemingjenkin_floor0))
                .anchor(0,1)
                .bearing(-32.75f)
                .position(new LatLng(55.92183131633249, -3.1726023819947824), 94.3f, 94.3f));

        hbFloor0 = map.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.hudsonbeare_floor0))
                .anchor(1,1)
                .bearing(-32.75f)
                .position(new LatLng(55.922364636983765, -3.170699665944885), 53.5f, 57f));

        sandFloor0 = map.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.sanderson_floor0))
                .anchor(1,0)
                .bearing(-32.75f)
                .position(new LatLng(55.923426729009286, -3.171759210993783), 64, 64));

        transparencyBar.setOnSeekBarChangeListener(this);

        /*
        approximate level of detail you can expect to see at each zoom level:
        1: World
        5: Landmass/continent
        10: City
        15: Streets
        20: Buildings*/

        //move the camera towards the initial point
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(KB, 17));

        //set transparency level of the floor plan
        flFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);
        sandFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);
        hbFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);

        // Display traffic.
        map.setTrafficEnabled(true);

    }

    //transparency slider
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (flFloor0 != null) {
            flFloor0.setTransparency((float) i / (float) TRANSPARENCY_MAX);
        }

        if (sandFloor0 != null) {
            sandFloor0.setTransparency((float) i / (float) TRANSPARENCY_MAX);
        }

        if (hbFloor0 != null) {
            hbFloor0.setTransparency((float) i / (float) TRANSPARENCY_MAX);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}