package edu.njit.map4noise.activities;

import edu.njit.map4noise.classes.LabelAlert;
import edu.njit.map4noise.classes.LayerAlert;
import edu.njit.map4noise.services.Audient;
import edu.njit.map4noise.views.RoundProgressBar;
import edu.njit.map4noise.classes.SoundMeter;
import edu.njit.map4noise.classes.SendDataThread;

import com.example.yuan.map4noise.R;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;


public class SoundMap extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    String name = null;
    public double calibration = 5.0;
    private int duration = 8000;
    private int backoff = 10000;

    private File audioFile = null;

    //http://128.235.40.185:8080/MyWebAppTest/ReturnData
    private GoogleMap mMap;
    //private MapView mMapView;
    private SupportMapFragment mMapFragment;
    private final int MIN_ZOOM = 11;
    private final int MAX_ZOOM = 17;

    private boolean showTwoHour = false;
    private boolean showLden = true;

    private boolean uploadFlag = false;

    private boolean serviceFlag = false;

    private View mLayerView;

    private CheckBox mCheckLden;
    private CheckBox mCheckTwoHour;
    private CheckBox mCheckEvent;
    private CheckBox mCheckSource;
    private LayerAlert mLayerAlert;

    private Button mBtnMeasure;
    private Button mBtnCollapse;
    private Button mBtnMonitor;
    private TextView mDecibelView;
    private TextView mMonitoringView;
    private SlidingUpPanelLayout mSlidingLayout;
    private RoundProgressBar mRoundProgressBar;
    private Button mBtnStartMonitor;
    private Button mBtnStopMonitor;

    private Button mBtnLocation;
    private Button mBtnLayer;
    private Button mBtnQuestion;
    private Button mBtnProfile;
    private Button mBtnFriends;
    private Button mBtnSetting;
    //
    //private HashMap<LatLng,Float> gridDataCache = new HashMap<LatLng,Float>();
    private HashSet<LatLng> gridDataCache = new HashSet<LatLng>();
    private HashMap<LatLng, Float> gridData = new HashMap<LatLng, Float>();
    private double east, west, south, north;

    //
    private final int GRID_DATA_CACHE_LIMIT = 1500;
    private final LatLng SOUTHWEST = new LatLng(40.65, -74.25);
    private final LatLng SOUTHEAST = new LatLng(40.65, -73.85);
    private final LatLng NORTHEAST = new LatLng(40.95, -73.85);
    private final LatLng NORTHWEST = new LatLng(40.95, -74.25);

    private Timer timer;
    private SoundMeter mMeter = null;

    double currentLatLon[] = {91.0, 181.0};
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private int locationChangeCount = 0;

    private float dBA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        setContentView(R.layout.activity_maps);
        init();
    }

    private void init() {

        //Get current username
        Intent intent = getIntent();
        if (intent != null) {
            name = intent.getStringExtra("username");
            String from = intent.getStringExtra("from");
            if (from.equals("Audient")) {
                this.serviceFlag = true;
            }
        }

        SQLiteDatabase db = openOrCreateDatabase("recentUser.db", this.MODE_PRIVATE, null);
        if(name==null) {
            Cursor c = db.rawQuery("SELECT * FROM person WHERE id = ?", new String[]{"1"});
            while (c.moveToNext()) {
                name = c.getString(c.getColumnIndex("name"));
                Log.i("db", "name => " + name);
            }
            c.close();
        }
        db.close();
//        if (name != null) {
//            db.execSQL("CREATE TABLE IF NOT EXISTS person (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR)");
//            db.execSQL("REPLACE INTO person (id, name) VALUES(1, ?)", new String[]{name});
//        } else {
//            Cursor c = db.rawQuery("SELECT * FROM person WHERE id = ?", new String[]{"1"});
//            while (c.moveToNext()) {
//                name = c.getString(c.getColumnIndex("name"));
//                Log.i("db", "name => " + name);
//            }
//            c.close();
//            db.close();
//        }



        //Initial checkboxes and button
//        mCheckLden = (CheckBox) findViewById(R.id.checkLden);
//        mCheckTwoHour = (CheckBox) findViewById(R.id.checkTwoHour);
//        mCheckEvent = (CheckBox) findViewById(R.id.checkEvent);
//        mCheckSource = (CheckBox) findViewById(R.id.checkSource);


        //initial all buttons
        mBtnMeasure = (Button) findViewById(R.id.btnRecord);
        mBtnCollapse =  (Button) findViewById(R.id.btnCollapse);
        mBtnMonitor = (Button) findViewById(R.id.btnMonitor);
        mRoundProgressBar = (RoundProgressBar) findViewById(R.id.roundProgressBar);
        mDecibelView = (TextView) findViewById(R.id.decibelView);
        mMonitoringView = (TextView) findViewById(R.id.monitoringView);
        mBtnStartMonitor = (Button) findViewById(R.id.btnStartMonitor);
        mBtnStopMonitor = (Button) findViewById(R.id.btnStopMonitor);
        mBtnLocation = (Button) findViewById(R.id.btnLocation);
        mBtnLayer = (Button) findViewById(R.id.btnLayer);
        mBtnQuestion = (Button) findViewById(R.id.btnQuestion);
        mBtnProfile = (Button) findViewById(R.id.btnProfile);
        mBtnFriends = (Button) findViewById(R.id.btnFriends);
        mBtnSetting = (Button) findViewById(R.id.btnSetting);

        mLayerView = View.inflate(this, R.layout.dialog_layer, null);
        mCheckLden = (CheckBox) mLayerView.findViewById(R.id.checkLden);
        mCheckTwoHour = (CheckBox) mLayerView.findViewById(R.id.checkTwoHour);
        mCheckEvent = (CheckBox) mLayerView.findViewById(R.id.checkEvent);
        mCheckSource = (CheckBox) mLayerView.findViewById(R.id.checkSource);

        FragmentManager fm = getSupportFragmentManager();
        mMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.mapView);
        mMap = mMapFragment.getMap();
        mMap.setMyLocationEnabled(true);


        //Set default UI of Google Map
        UiSettings us = mMap.getUiSettings();

        us.setZoomControlsEnabled(false);
        us.setZoomGesturesEnabled(true);
        us.setScrollGesturesEnabled(true);
        us.setCompassEnabled(true);
        us.setRotateGesturesEnabled(false);
        us.setTiltGesturesEnabled(false);
        us.setMyLocationButtonEnabled(false);

        //initial the bounds of sound data query;
        east = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter().longitude;
        north = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter().latitude;
        west = east;
        south = north;

        //Draw the BOUNDS of monitoring area
        drawBounds();


        //Set listener for camera view change
        mMap.setOnCameraChangeListener(onCameraChangeListener);

        //Set listener for map mode choice
        mCheckLden.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCheckLden.isChecked()) {
                    mCheckTwoHour.setChecked(false);
                    showLden = true;
                    showTwoHour = false;
                    Toast.makeText(SoundMap.this,
                            "Zoom in to see Lden map",
                            Toast.LENGTH_SHORT).show();
                } else if (!mCheckLden.isChecked()) {
                    showLden = false;
                }
            }
        });
        mCheckTwoHour.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCheckTwoHour.isChecked()) {
                    mCheckLden.setChecked(false);
                    showLden = false;
                    showTwoHour = true;
                    Toast.makeText(SoundMap.this,
                            "Zoom in to see two-hour dBA map",
                            Toast.LENGTH_SHORT).show();
                } else if (!mCheckTwoHour.isChecked()) {
                    showTwoHour = false;
                }
            }
        });

        //Sliding Up panel and its Listener
        mSlidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingLayout.setAnchorPoint(0.55f);
        mSlidingLayout.setTouchEnabled(false);
        mSlidingLayout.setPanelSlideListener(
            new SlidingUpPanelLayout.PanelSlideListener() {
                //        final String TAG = "onPanelSlide";
                @Override
                public void onPanelSlide(View panel, float slideOffset) {
//                        Log.i(TAG, "onPanelSlide, offset " + slideOffset);
//                        Log.i(TAG, "main height=" + mSlidingLayout.findViewById(R.id.main).getHeight());
                }

                @Override
                public void onPanelExpanded(View panel) {
                    mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                }

                @Override
                public void onPanelCollapsed(View panel) {
                    mBtnMeasure.setVisibility(View.VISIBLE);
                    mBtnMeasure.setClickable(true);
                    mBtnStartMonitor.setVisibility(View.VISIBLE);
                    mBtnStartMonitor.setClickable(true);
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                    mSlidingLayout.setTouchEnabled(false);
                }

                @Override
                public void onPanelAnchored(View panel) {
                    //mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    mSlidingLayout.setTouchEnabled(true);
                }

                @Override
                    public void onPanelHidden(View panel) {
                        //timer.cancel();
                }
            }
        );

        // Progress Bar
        final int[] progress = {0};
        final Thread[] soundMeterThread = {null};
        //Collapse Button Listener
        mBtnCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnMeasure.setVisibility(View.VISIBLE);
                mBtnMeasure.setClickable(true);
                //mBtnCollapse.setClickable(false);
                //timer.cancel();
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                if (progress[0] == 0) mRoundProgressBar.setProgress(0);
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });
        //Expand, One Time Measure Button Listener
        mBtnMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean pendingServiceFlag[] = {false};
                if(serviceFlag) {
                    //Toast.makeText(SoundMap.this, "Monitor service suspended!", Toast.LENGTH_LONG).show();
                    serviceFlag = false;
                    pendingServiceFlag[0] = true;
                    Intent intent = new Intent(SoundMap.this, Audient.class);
                    stopService(intent);
                }
                //mBtnCollapse.setClickable(false);
                //mBtnMeasure.setClickable(false);
                //mBtnMeasure.setVisibility(View.INVISIBLE);
                mMap.getUiSettings().setScrollGesturesEnabled(false);
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                mDecibelView.setVisibility(View.VISIBLE);
                mMonitoringView.setVisibility(View.INVISIBLE);
                mBtnMeasure.setClickable(false);
                mRoundProgressBar.setVisibility(View.VISIBLE);
                mBtnStartMonitor.setVisibility(View.INVISIBLE);
                mBtnStopMonitor.setVisibility(View.INVISIBLE);

                updateLocation();

                double lat = currentLatLon[0], lon = currentLatLon[1];
                if (40.65 < lat && lat < 40.95 && -74.25 < lon && lon < -73.85) {
                    uploadFlag = true;
                } else {
                    uploadFlag = false;
                    Toast.makeText(SoundMap.this,
                            "You cannot upload noise data since your current location",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                uploadFlag = true;

                //Start to record and analyze sound
                //Set progress bar while record
                if (soundMeterThread[0] == null) {
                    mMeter = new SoundMeter(mMeterHandler, mCurrentDecibelHandler, calibration, duration);
                    soundMeterThread[0] = new Thread(mMeter);
                    soundMeterThread[0].start();
                    if (timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            if (progress[0] <= 100) {
                                mRoundProgressBar.setProgress(progress[0]);
                                progress[0]++;
                            } else {
                                if (pendingServiceFlag[0]) {
                                    Intent intent = new Intent(SoundMap.this, Audient.class);
                                    intent.putExtra("username", name);
                                    intent.putExtra("duration", duration);
                                    intent.putExtra("backoff", backoff);
                                    startService(intent);
                                    serviceFlag = true;
                                    //Toast.makeText(SoundMap.this, "Monitor service recovered!", Toast.LENGTH_LONG).show();
                                }
                                //mRoundProgressBar.setVisibility(View.INVISIBLE);
                                //mBtnCollapse.setClickable(true);
                                progress[0] = 0;
                                soundMeterThread[0] = null;
                                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                timer.cancel();
                            }
                        }
                    }, 0, duration / 100);
                }
            }
        });
        mBtnMonitor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                mMonitoringView.setVisibility(View.VISIBLE);
                mRoundProgressBar.setVisibility(View.INVISIBLE);
                mDecibelView.setVisibility(View.INVISIBLE);
                if(!serviceFlag) {
                    mBtnStartMonitor.setVisibility(View.VISIBLE);
                    mBtnStartMonitor.setClickable(true);
                    mBtnStopMonitor.setVisibility(View.INVISIBLE);
                    mBtnStopMonitor.setClickable(false);
                } else {
                    mBtnStartMonitor.setVisibility(View.INVISIBLE);
                    mBtnStartMonitor.setClickable(false);
                    mBtnStopMonitor.setVisibility(View.VISIBLE);
                    mBtnStopMonitor.setClickable(true);

                }
            }
        });
        mBtnStartMonitor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                if(soundMeterThread[0] != null) {
                    timer.cancel();
                    progress[0] = 0;
                    mMeter.stop();
                    mMeter = null;
                    soundMeterThread[0] = null;
                }
                if(!serviceFlag) {
                    Toast.makeText(SoundMap.this, "Monitor service started!", Toast.LENGTH_LONG).show();
                    serviceFlag = true;
                    Intent intent = new Intent(SoundMap.this, Audient.class);
                    intent.putExtra("username", name);
                    intent.putExtra("duration", duration);
                    intent.putExtra("backoff", backoff);
                    startService(intent);
                }
            }
        });
        mBtnStopMonitor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                if(serviceFlag) {
                    Toast.makeText(SoundMap.this, "Monitor service stopped!", Toast.LENGTH_LONG).show();
                    serviceFlag = false;
                    mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    Intent intent = new Intent(SoundMap.this, Audient.class);
                    stopService(intent);
                }
            }
        });
        mBtnLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(SoundMap.this,"location",Toast.LENGTH_SHORT);
                CameraPosition position = mMap.getCameraPosition();
                CameraPosition newPosition = new CameraPosition(new LatLng(currentLatLon[0], currentLatLon[1]), position.zoom, position.tilt, position.bearing);
                CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
                mMap.moveCamera(update);
            }
        });
        mBtnLayer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(SoundMap.this,"layers",Toast.LENGTH_SHORT);
                mLayerAlert = new LayerAlert(SoundMap.this, mLayerView);
            }
        });
    }


    private void initCamera(){
        for(int i = 0; i < 10; i++){
            updateLocation();
            if(currentLatLon [0] != 91) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (40.65 < currentLatLon [0] && currentLatLon[0] < 40.95 && -74.25 < currentLatLon[1] && currentLatLon[1] < -73.85) {
            CameraPosition newPosition = new CameraPosition(new LatLng(currentLatLon[0], currentLatLon[1]), 13, 0, (float) 0.0);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
            mMap.moveCamera(update);
            uploadFlag = true;
        }
        else {
            CameraPosition newPosition = new CameraPosition(new LatLng(40.80, -74.05), 13, 0, (float) 0.0);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
            mMap.moveCamera(update);
        }
    }

    private void updateLocation() {
        Location lastLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(lastLoc != null){
            currentLatLon[0] = lastLoc.getLatitude();
            currentLatLon[1] = lastLoc.getLongitude();
        } else {
            //lastLoc is null;
            System.out.println("Last location is null");
        }
        checkLocation();
    }

    public void checkLocation() {
        if (40.65 < currentLatLon [0] && currentLatLon[0] < 40.95 && -74.25 < currentLatLon[1] && currentLatLon[1] < -73.85) {
            if((locationChangeCount)==0){
                CameraPosition newPosition = new CameraPosition(new LatLng(currentLatLon[0], currentLatLon[1]), 13, 0, (float) 0.0);
                CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
                mMap.moveCamera(update);
                locationChangeCount++;
            }
            uploadFlag = true;
        }
        else {
            //Toast.makeText(SoundMap.this, "You cannot upload noise data since your current location", Toast.LENGTH_LONG).show();
            //CameraPosition newPosition = new CameraPosition(new LatLng(40.80, -74.05), 13, 0, (float) 0.0);
            //CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
            //mMap.moveCamera(update);
        }
    }



    /**
     * Camera View Change Listener:
     * 1.When Zoom is greater or equal than 17, draw sound map masking for map;
     * 2. When Zoom is less or equal than 13, clear the sound map mask from view;
     * 3. Constrain area and zoom;
     */
    GoogleMap.OnCameraChangeListener onCameraChangeListener =
            new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    boolean requestFlag = false;
                    //Limit area and zoom
                    //limitZoneAndZoom();
                    LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    //if zoom level is either 17 or 18;
                    //if zoom level is 14 or smaller, clear sound mask;
                    if (mMap.getCameraPosition().zoom >= 14) {
                        if (visibleBounds.southwest.latitude < south || visibleBounds.southwest.longitude < west) {
                            requestFlag = true;
                        }
                        if (visibleBounds.northeast.latitude > north || visibleBounds.northeast.longitude > east) {
                            requestFlag = true;
                        }
                    } else if(mMap.getCameraPosition().zoom < 14) {
                        clearSoundMask();
                    }
                    if(gridDataCache.size() > GRID_DATA_CACHE_LIMIT){
                        clearSoundMask();
                    }
                    if (requestFlag){
                        south = visibleBounds.southwest.latitude - 0.009;//the boundry of sound mask
                        north = visibleBounds.northeast.latitude + 0.009;//the boundry of sound mask
                        west = visibleBounds.southwest.longitude - 0.012;//the boundry of sound mask
                        east = visibleBounds.northeast.longitude + 0.012;//the boundry of sound mask
                        new Thread(new RequestDataThread()).start();
                        //System.gc();
                    }
                }
            };


    /**
     *
     */
    private void drawBounds() {
        PolylineOptions rectOptions = new PolylineOptions().add(
                SOUTHWEST,
                SOUTHEAST,
                NORTHEAST,
                NORTHWEST,
                SOUTHWEST);
        Polyline polyline = mMap.addPolyline(rectOptions
                .color(Color.BLUE)
                .width(3));
        polyline.setVisible(true);
    }


    /**
     * Add sound mask polygons
     */
    private void drawSoundMask(){
        double d1=0.0004f, d2=0.0003f;
        Iterator iter = gridData.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            LatLng key = (LatLng) entry.getKey();
            float val = (float) entry.getValue();
            PolygonOptions rectOptions = new PolygonOptions().add(
                    new LatLng((2*key.latitude - d2)/2, (2*key.longitude - d1)/2),
                    new LatLng((2*key.latitude + d2)/2, (2*key.longitude - d1)/2),
                    new LatLng((2*key.latitude + d2)/2, (2*key.longitude + d1)/2),
                    new LatLng((2*key.latitude - d2)/2, (2*key.longitude + d1)/2),
                    new LatLng((2*key.latitude - d2)/2, (2*key.longitude - d1)/2));
            if(val > 50) {
                if (val <= 100) {
                    Polygon polygon = mMap.addPolygon(rectOptions
                            .fillColor(Color.argb((int) (3 * val - 110), (int) ((val) * 2.55f), (int) (255.0f - (val - 50.0f) * 5.1f), 0))
                            .strokeColor(Color.argb(0, 255, 255, 255))
                            .strokeWidth(0));
                    polygon.setVisible(true);
                } else {
                    Polygon polygon = mMap.addPolygon(rectOptions
                            .fillColor(Color.argb((int)(3 * val - 110), 255, 0, 0))
                            .strokeColor(Color.argb(0, 255, 255, 255))
                            .strokeWidth(0));
                    polygon.setVisible(true);
                }
            } else {
                // If sound is smaller than 50 db, do not draw its polygon out.
            }
        }
    }


    /**
     * clear the sound mask (called when zoom in)
     */
    private void clearSoundMask(){
        mMap.clear();
        gridDataCache.clear();
        gridData.clear();
        east = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter().longitude;
        north = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter().latitude;
        west = east;
        south = north;
        drawBounds();
        System.gc();
    }


    /**
     * Limit the area shown in app to a rectangular area including New York City;
     */
    private void limitZoneAndZoom(){
        CameraPosition position = mMap.getCameraPosition();
        VisibleRegion region = mMap.getProjection().getVisibleRegion();
        float zoom = 0;
        if(position.zoom < MIN_ZOOM) zoom = MIN_ZOOM;
        if(position.zoom > MAX_ZOOM) zoom = MAX_ZOOM;
        LatLng correction = getLatLngCorrection(region.latLngBounds);
        if(zoom != 0 || correction.latitude != 0 || correction.longitude != 0) {
            zoom = (zoom==0) ? position.zoom : zoom;
            double lat = position.target.latitude + correction.latitude;
            double lon = position.target.longitude + correction.longitude;
            CameraPosition newPosition = new CameraPosition(new LatLng(lat,lon), zoom, position.tilt, position.bearing);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(newPosition);
            mMap.moveCamera(update);
        }
    }


    /**
     * Returns the correction for Lat and Lng if camera is trying to get outside of visible map
     * @param cameraBounds Current camera bounds
     * @return Latitude and Longitude corrections to get back into bounds.
     */
    private LatLng getLatLngCorrection(LatLngBounds cameraBounds) {
        double latitude=0, longitude=0;
        if(cameraBounds.southwest.latitude < SOUTHWEST.latitude) {
            latitude =SOUTHWEST.latitude - cameraBounds.southwest.latitude + 0.0002;
        }
        if(cameraBounds.southwest.longitude < SOUTHWEST.longitude) {
            longitude = SOUTHWEST.longitude - cameraBounds.southwest.longitude + 0.0002;
        }
        if(cameraBounds.northeast.latitude > NORTHEAST.latitude) {
            latitude = NORTHEAST.latitude - cameraBounds.northeast.latitude - 0.0002;
        }
        if(cameraBounds.northeast.longitude > NORTHEAST.longitude) {
            longitude = NORTHEAST.longitude - cameraBounds.northeast.longitude - 0.0002;
        }
        return new LatLng(latitude, longitude);
    }


    Handler mSendDataHandler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String result = data.getString("result");
            if (!result.equals("0")){
                dBA = (float) data.getDouble("dBA");
                float lat = (float) data.getDouble("lat");
                float lon = (float) data.getDouble("lon");
                Toast.makeText(SoundMap.this, "Username: " + name + ", Lat: " + lat + ", Lon: " + lon + ", dBA: " + dBA, Toast.LENGTH_SHORT).show();
                audioFile.delete();
                audioFile = null;
                new LabelAlert(SoundMap.this, result);
            } else {
                Toast.makeText(SoundMap.this, "Fail to upload data.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    Handler mMeterHandler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            dBA = (float) data.getDouble("dBA");
            if(dBA!=0) {
                audioFile = new File(data.getString("audioFile"));
                System.out.println(audioFile.toString());
                //Toast.makeText(SoundMap.this, "Username: " + name + ", Lat: " + currentLatLon[0] + ", Lon: " + currentLatLon[1] + ", dBA: " + dBA, Toast.LENGTH_SHORT).show();
                new Thread(new SendDataThread(SoundMap.this, dBA, currentLatLon[1], currentLatLon[0], name, mSendDataHandler, audioFile)).start();
            } else {
                return;
            }
        }
    };

    Handler mCurrentDecibelHandler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            DecimalFormat df = new DecimalFormat("0.00");
            mDecibelView.setText(df.format(data.getDouble("dBA")) + " dBA");
        }
    };

    /**
     *
     */
    Handler mRequestDataHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String jsonArrayString = data.getString("soundData");
            gridData.clear();
            try {
                JSONArray jsonArray = new JSONArray(jsonArrayString);
                String [] stringArray = new String [3];
                for (int index = 0; index < jsonArray.length(); index++){
                    JSONObject jsonObject = jsonArray.getJSONObject(index);
                    Iterator<?> iterator = jsonObject.keys();
                    String key = null;
                    String value = null;
                    while (iterator.hasNext()) {
                        key = (String) iterator.next();
                        value = jsonObject.getString(key);
                        if (key.equals("db")){
                            stringArray[0] = value;
                        } else if (key.equals("lon")){
                            stringArray[1] = value;
                        } else if (key.equals("lat")){
                            stringArray[2] = value;
                        }
                    }
                    LatLng temp = new LatLng(Float.parseFloat(stringArray[2]), Float.parseFloat(stringArray[1]));
                    if (!gridDataCache.contains(temp) && Float.parseFloat(stringArray[0]) > 50) {
                        gridData.put(temp, Float.parseFloat(stringArray[0]));
                        gridDataCache.add(temp);
                    }
                }
            } catch (JSONException e) {
                return;
            }
            //Start process UI
            drawSoundMask();
        }
    };


    /**
     * Get the sound map data within a rectangular zone
     * from servlet through Http post connection
     */
    class RequestDataThread implements Runnable{
        @Override
        public void run() {
            // TODO: http post.
            String result = "0";
            //Get the instance of ClosealbeHttpClient
            CloseableHttpClient httpClient = HttpClients.createDefault();
            //The url of servlet
            //String url = "https://web.njit.edu/~yl768/webapps7/ReturnData";
            //String url = "http://128.235.40.185:8080/MyWebAppTest/ReturnData";
            String url = "https://map4noise.njit.edu/ReturnData.php";
            //New HTTP Post request
            HttpPost httpPost = new HttpPost(url);
            //Add Name Value Pairs to HTTP request
            NameValuePair pair1 = new BasicNameValuePair("east", "" + east);
            NameValuePair pair2 = new BasicNameValuePair("west", "" + west);
            NameValuePair pair3 = new BasicNameValuePair("south", "" + south);
            NameValuePair pair4 = new BasicNameValuePair("north", "" + north);
            ArrayList<NameValuePair> pairs = new ArrayList<>();
            pairs.add(pair1);
            pairs.add(pair2);
            pairs.add(pair3);
            pairs.add(pair4);
            //Send Http post request
            try {
                HttpEntity requestEntity = new UrlEncodedFormEntity(pairs);
                httpPost.setEntity(requestEntity);
                CloseableHttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(entity.getContent()));
                    result = reader.readLine();
                    //Log.d("HTTP", "POST:" + result);
                } else {
                    result = "" + response.getStatusLine().getStatusCode();
                    //Log.d("HTTP", "ERROR:" + result);
                }
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                Toast.makeText(SoundMap.this,"Cannot access server, please connect NJIT LAN or VPN.",Toast.LENGTH_LONG).show();
                e2.printStackTrace();
            }
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("soundData", result);
            msg.setData(data);
            mRequestDataHandler.sendMessage(msg);
        }
    }

/*
    class SendDataThread implements Runnable {
        private double dBA;
        public SendDataThread(double dBA){
            this.dBA = dBA;
        }
        @Override
        public void run() {
            // TODO: http post.
            String result = "0";
            //Get the instance of ClosealbeHttpClient
            HttpClient httpClient = HttpClients.createDefault();
            //The url of servlet
            //String url = "https://web.njit.edu/~yl768/webapps7/ReceiveData";
            String url = "http://128.235.40.185:8080/MyWebAppTest/ReceiveData1";
            //New HTTP Post request
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);//设置浏览器兼容模式
            builder.addBinaryBody("audioFile", audioFile, ContentType.DEFAULT_BINARY, "sampling.wav");
            builder.addTextBody("lon", "" + currentLatLon[1]);
            builder.addTextBody("lat", "" + currentLatLon[0]);
            builder.addTextBody("dB", "" + dBA);
            builder.addTextBody("user", "" + name);
            builder.addTextBody("fileName", audioFile.toString());
            //Add Name Value Pairs to HTTP request
//            String parameters = "{\"lon\":\"" + currentLatLon[1]
//                    + "\",\"lat\":\"" + currentLatLon[0]
//                    + "\",\"dB\":\"" + dBA
//                    + "\",\"user\":\"" + name
//                    + "\",\"fileName\":\"" + audioFile.toString() + "\"}";
//            builder.addTextBody("parameters", parameters);
//            NameValuePair pair1 = new BasicNameValuePair("lon", "" + currentLatLon[1]);
//            NameValuePair pair2 = new BasicNameValuePair("lat", "" + currentLatLon[0]);
//            NameValuePair pair3 = new BasicNameValuePair("dB", "" + dBA);
//            NameValuePair pair4 = new BasicNameValuePair("user", "" + name);
//            NameValuePair pair5 = new BasicNameValuePair("fileName", audioFile.toString());
//            ArrayList<NameValuePair> pairs = new ArrayList<>();
//            pairs.add(pair1);
//            pairs.add(pair2);
//            pairs.add(pair3);
//            pairs.add(pair4);
//            pairs.add(pair5);

            //Send Http post request
            try {
                HttpEntity httpEntity = builder.build();
                //HttpEntity httpEntity = new UrlEncodedFormEntity(pairs);
                httpPost.setEntity(httpEntity);
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(entity.getContent()));
                    result = reader.readLine();
                    Log.d("HTTP", "POST:" + result);
                } else {
                    result = "" + response.getStatusLine().getStatusCode();
                    Log.d("HTTP", "ERROR:" + result);
                }
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                Toast.makeText(SoundMap.this,"Cannot access server, please connect NJIT LAN or VPN.",Toast.LENGTH_LONG).show();
                e2.printStackTrace();
            }
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("result", result);
            data.putDouble("lon", currentLatLon[1]);
            data.putDouble("lat", currentLatLon[0]);
            data.putDouble("dBA", dBA);
            msg.setData(data);
            mSendDataHandler.sendMessage(msg);
        }
    }
*/

//    /**
//     *
//     * @return
//     */
//    private double[] getCurrentLatLon (){
//        double currentLatLon [] = {0, 0};
//        List<String> providers = locationManager.getProviders(true);
//        if (providers.contains(LocationManager.GPS_PROVIDER)) {
//            //If is GPS
//            locationProvider = LocationManager.GPS_PROVIDER;
//        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
//            //If is Network
//            locationProvider = LocationManager.NETWORK_PROVIDER;
//        } else {
//            Toast.makeText(this, "No location provider", Toast.LENGTH_SHORT).show();
//            currentLatLon[0] = 91; //91 is illegal for latitude
//            currentLatLon[1] = 181;//181 is illegal for longitude
//            return currentLatLon;
//        }
//        //
//        //获取Location
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(SoundMap.this, "Don't have permission to access location ", Toast.LENGTH_SHORT).show();
//                return currentLatLon;
//            }
//        }
//        Location location = locationManager.getLastKnownLocation(locationProvider);
//        if (location!=null) {
//
//            currentLatLon[0] = location.getLatitude();
//            currentLatLon[1] = location.getLongitude();
//        }
//        return currentLatLon;
//    }


    @Override
    protected void onStart() {
        // Connect the client.
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mMapView.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mMapView.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        String name1 = name;
        int duration1 = duration;
        int backoff1 = backoff;
        super.onDestroy();
        //前台程序销毁的时候,后台进程如何处理呢?
//        if(serviceFlag){
//            Intent intent = new Intent(SoundMap.this, Audient.class);
//            stopService(intent);
//            intent = new Intent(SoundMap.this, Audient.class);
//            intent.putExtra("username", name1);
//            intent.putExtra("duration", duration1);
//            intent.putExtra("backoff", backoff1);
//            startService(intent);
//        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("SoundMap", "GoogleApiClient connected");
        int permissionCheck = ContextCompat.checkSelfPermission(SoundMap.this,
                Manifest.permission.WRITE_CALENDAR);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(300*1000); // Update location every second
            mLocationRequest.setFastestInterval(1*1000); // Update location every second
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        initCamera();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("SoundMap", "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("SoundMap", "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        //获取Location
        Log.i("SoundMap", "Location changed");
        int permissionCheck = ContextCompat.checkSelfPermission(SoundMap.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            currentLatLon[0] = location.getLatitude();
            currentLatLon[1] = location.getLongitude();
            //Toast.makeText(SoundMap.this, "" + currentLatLon[0] + currentLatLon[1], Toast.LENGTH_SHORT).show();
            checkLocation();
        }
    }


}