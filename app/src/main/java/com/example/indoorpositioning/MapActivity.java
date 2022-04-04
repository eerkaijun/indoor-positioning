//By Salman Saiful Redzuan
/*
This activity simply shows the map, that includes the ground floor plan of Fleeming Jenkin, Hudson Beare and Sanderson Building, and our location.
It uses step detection method to determines the next location. There's also a trail of where you have walked inside the building.
 */

package com.example.indoorpositioning;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener, MovementDetection.OnMovementDetectionManagerListener {

    //sensor manager
    private MovementDetection movementDetection;

    // get access to wifi
    private WifiManager wifiManager;

    //Create a point to initialise where camera points at the beginning
    private static final LatLng KB = new LatLng(55.92257794064054, -3.1718609539182485);

    //polyline and its style
    private Polyline polyLine;
    private static final int COLOR_BLUE_ARGB = 0xdb2488e5;
    private static final int POLYLINE_STROKE_WIDTH_PX = 7;

    //marker for current location
    private Marker pointer;
    private float headingAngle = 0.0f;
    ////To change after the algorithm works
    private LatLng currLocation = new LatLng(55.9226569, -3.1727689);

    private static final int TRANSPARENCY_MAX = 100;
    private static final int TRANSPARENCY_DEF = 65;

    //UI features
    private SeekBar transparencyBar;
    private FloatingActionButton recenter;
    private ToggleButton tracking;

    private CameraPosition def_camera_position = new CameraPosition.Builder()
            .target(currLocation)                   // Sets the center of the map to our current location
            .bearing(headingAngle)                  // Sets the orientation of the camera to where the device is facing
            .tilt(30)                               // Sets the tilt of the camera to 30 degrees
            .zoom(22)                               //set zoom to 22
            .build();                               // Creates a CameraPosition from the builder;
    private boolean track = true;

    //ground overlay objects
    private GroundOverlay flFloor0;
    private GroundOverlay hbFloor0;
    private GroundOverlay sandFloor0;

    private GoogleMap map;

    // Wifi access point along the pathway
    Map<String, List<Double>> wifiAccess = new HashMap(){{
        put("54:78:1a:21:3b:ce", new ArrayList(){{
            add(-3.172213655307299);
            add(55.92286945847802);
        }});
        put("58:97:1e:14:75:51", new ArrayList(){{
            add(-3.172063583747593);
            add(55.922772971452424);
        }});
        put("58:97:1e:14:75:5e", new ArrayList(){{
            add(-3.1717719704271743);
            add(55.92249806304488);
        }});
        put("58:97:1e:15:11:61", new ArrayList(){{
            add(-3.1717237939583662);
            add(55.92242503283533);
        }});
        put("5c:83:8f:dd:6b:21", new ArrayList(){{
            add(-3.1718986409867176);
            add(55.922630579117);
        }});
        put("b8:3a:5a:ba:69:a0", new ArrayList(){{
            add(-3.172418424028353);
            add(55.92279054759158);
        }});
        put("b8:3a:5a:bb:5d:c0", new ArrayList(){{
            add(-3.1721805576293463);
            add(55.9228651820223);
        }});
        put("b8:be:bf:ef:1b:ee", new ArrayList(){{
            add(-3.1720864367647055);
            add(55.92280396412815);
        }});
        put("c4:0a:cb:25:13:ce", new ArrayList(){{
            add(-3.1718691679548563);
            add(55.922603367738695);
        }});
    }};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // initialise wifi
        wifiManager =  (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

        movementDetection = new MovementDetection(this);
        movementDetection.setOnMovementDetectionManagerListener(this);

        //setup the transparency slider
        transparencyBar = findViewById(R.id.transparencySeekBar);
        transparencyBar.setMax(TRANSPARENCY_MAX);
        transparencyBar.setProgress(TRANSPARENCY_DEF);

        //set floating button
        recenter = findViewById(R.id.floatingActionButton);

        //set toggle button
        tracking = findViewById(R.id.toggleButton);
        tracking.setChecked(true);

        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register sensors
        movementDetection.registerSensors();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregister sensors when unused
        movementDetection.unregisterSensors();
        unregisterReceiver(wifiScanReceiver);
    }

    // When map is ready, do
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

        //set default transparency level of the floor plan
        flFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);
        sandFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);
        hbFloor0.setTransparency((float)TRANSPARENCY_DEF/TRANSPARENCY_MAX);

        transparencyBar.setOnSeekBarChangeListener(this);

        /*
        approximate level of detail you can expect to see at each zoom level:
        1: World
        5: Landmass/continent
        10: City
        15: Streets
        20: Buildings*/

        // Construct a CameraPosition focusing on Kings Buildings and animate the camera to that position.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(KB)                // Sets the center of the map to KB
                .zoom(18)                   // Sets the zoom
                .bearing(0)                 // Sets the orientation of the camera to north
                .tilt(0)                    // Sets the tilt of the camera to 0 degrees
                .build();                   // Creates a CameraPosition from the builder

        //move the camera towards the initial point
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 4000, null);

        // Display traffic.
        map.setTrafficEnabled(true);

        //add location marker
        pointer = map.addMarker(new MarkerOptions()
                .position(currLocation)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_arrow)));

        //add trail
        polyLine = map.addPolyline(new PolylineOptions()
                .add(currLocation));

        polyLine.setStartCap(new RoundCap());
        polyLine.setEndCap(new RoundCap());
        polyLine.setJointType(JointType.ROUND);
        polyLine.setColor(COLOR_BLUE_ARGB);
        polyLine.setWidth(POLYLINE_STROKE_WIDTH_PX);

        //recenter button click
        recenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(def_camera_position));
            }
        });

        //tracking switch
        tracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    track = true;
                }
                else {
                    track = false;
                }
            }
        });
    }

    @Override
    public void onHeadingUpdated(float degree) {
        headingAngle = degree;
        if (pointer != null) {
            pointer.setRotation(headingAngle);
        }
    }

    int step = 0;
    @Override
    public void onStepDetected(float lat, float lon) {
        currLocation = new LatLng(lat, lon);
        step++;
        CameraPosition cameraPosition;
        updateTrail(currLocation);
        pointer.setPosition(currLocation);

        def_camera_position = new CameraPosition.Builder()
                .target(currLocation)                   // Sets the center of the map to our current location
                .bearing(headingAngle)                  // Sets the orientation of the camera to where the device is facing
                .tilt(30)                               // Sets the tilt of the camera to 30 degrees
                .zoom(22)                               //set zoom to 22
                .build();                               // Creates a CameraPosition from the builder

        //if track button is on
        if (track) {
            //for the first step, set zoom to default
            if (step == 1) {
                cameraPosition = def_camera_position;
            }
            // for the step afterwards, set zoom and tilt to to whatever the user set
            else {
                cameraPosition = new CameraPosition.Builder()
                        .target(currLocation)                       // Sets the center of the map to our current location
                        .bearing(headingAngle)                      // Sets the orientation of the camera to where the device is facing
                        .tilt(map.getCameraPosition().tilt)         // Sets the tilt of the camera to user preference
                        .zoom(map.getCameraPosition().zoom)         //set zoom to user preference
                        .build();                                   // Creates a CameraPosition from the builder
            }
        }
        else {
            cameraPosition = new CameraPosition.Builder()
                    .target(map.getCameraPosition().target)     // Sets the center of the map to our current location
                    .bearing(map.getCameraPosition().bearing)   // Sets the orientation of the camera to where the device is facing
                    .tilt(map.getCameraPosition().tilt)         // Sets the tilt of the camera to user preference
                    .zoom(map.getCameraPosition().zoom)         //set zoom to user preference
                    .build();                                   // Creates a CameraPosition from the builder
        }
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }

    private void updateTrail(LatLng latLng) {
        List<LatLng> points = polyLine.getPoints();
        points.add(latLng);
        polyLine.setPoints(points);
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

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            // wifi fingerprinting only done for the first time, therefore unregister receiver after receiving results
            unregisterReceiver(this);

            String signalMAC = "";
            int strongestSignal = 0;
            for (int i=0; i<wifiScanList.size(); i++) {
                if (wifiAccess.containsKey(wifiScanList.get(i).BSSID) && wifiScanList.get(i).level * (-1) > strongestSignal) {
                    strongestSignal = wifiScanList.get(i).level * (-1);
                    signalMAC = wifiScanList.get(i).BSSID;
                    Log.d("Strongest signal", String.valueOf(strongestSignal));
                    Log.d("Wifi name", wifiScanList.get(i).SSID + '|' + wifiScanList.get(i).BSSID);
                }
            }
            if (wifiAccess.containsKey(signalMAC)) {
                Double latitude = wifiAccess.get(signalMAC).get(1);
                Double longitude = wifiAccess.get(signalMAC).get(0);
                currLocation = new LatLng(latitude, longitude);
            }
        }
    };
}