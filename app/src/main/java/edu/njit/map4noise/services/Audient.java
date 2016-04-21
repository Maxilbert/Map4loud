package edu.njit.map4noise.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;


import edu.njit.map4noise.activities.SoundMap;
import edu.njit.map4noise.classes.SendDataThread;
import edu.njit.map4noise.classes.SoundMeter;
import com.example.yuan.map4noise.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.lang.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Audient extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static int myID = 43178652;
    private static final String TAG = "Service Demo" ;
    private static final int NOTIFICATION_FLAG = 1;
    private int nSeq = 1;

    private static final int ONE_MINUTE = 60000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private PowerManager.WakeLock mWakeLock;

    private double lat = 91.0, lon = 181.0;
    private SoundMeter mMeter;
    private InfiniteMonitoringService monitoringService;
    //private Thread monitoringThread;

    private static String name;
    private double calibration = 5.0;
    private static int duration = 8000;
    private final int interval = ONE_MINUTE;
    private static int backoff = ONE_MINUTE;

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Service Demo onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Service Demo onCreate");
        super.onCreate();
        if (mGoogleApiClient == null)
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        Log.v(TAG, "Service Demo onStart");
        super.onStart(intent, startId);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        if(intent != null) {
            name = intent.getStringExtra("username");
            duration = intent.getIntExtra("duration", this.duration);
            backoff = intent.getIntExtra("backoff", this.backoff);
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
//                //Log.i("db", "name => " + name);
//            }
//            c.close();
//            db.close();
//        }

        System.out.println(name);
        monitoringService = new InfiniteMonitoringService();
        //monitoringThread = new Thread(monitoringService);
        //monitoringThread.start();
        //acquireWakeLock();
        //this.pushNotifications();
        notification();
        startForeground(myID, notification);
    }


    private Handler mMeterHandler = new Handler(){
        double dBA;
        File audioFile;
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            dBA = (float) data.getDouble("dBA");
            if(dBA != 0.0) {
                audioFile = new File(data.getString("audioFile"));
                System.out.println(audioFile.toString());
                //Toast.makeText(SoundMap.this, "Username: " + name + ", Lat: " + currentLatLon[0] + ", Lon: " + currentLatLon[1] + ", dBA: " + dBA, Toast.LENGTH_SHORT).show();
                new Thread(new SendDataThread(Audient.this, dBA, lon, lat, name, audioFile)).start();
            } else {
                return;
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service Demo onDestroyCommand");

        cancelNotification();
        //releaseWakeLock();
        mGoogleApiClient.disconnect();
        if(mMeter != null){
            mMeter.stop();
            mMeter = null;
        }
        monitoringService.stop();
        //monitoringThread = null;
        stopForeground(true);
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Service Demo onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }




    class InfiniteMonitoringService {

        ScheduledThreadPoolExecutor se;

        public InfiniteMonitoringService() {
            se = new ScheduledThreadPoolExecutor(2);
            scheduleTask();
        }

        public void scheduleTask(){
            se.schedule(new RecordTask(), backoff, TimeUnit.MILLISECONDS);
            //se.schedule(new RecordTask(), 60, TimeUnit.SECONDS);
            Log.v("Schedule", "Backoff is " + backoff);
        }

        public void stop(){
            if(mMeter != null) {
                mMeter.stop();
                mMeter = null;
            }
            if(se != null) {
                se.shutdownNow();
                se = null;
            }
        }

        class RecordTask implements Runnable {
            public void run() {
                mMeter = null;
                Random random = new Random();
                backoff = random.nextInt(ONE_MINUTE - 1) + ONE_MINUTE;
                scheduleTask();
                System.gc();
                mMeter = new SoundMeter(mMeterHandler, calibration, duration);
                new Thread(mMeter).start();
                try {
                    Thread.sleep(duration+100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }



//    Timer timer = null;

//    class InfiniteMonitoringService {
//        public InfiniteMonitoringService() {
//            timer = new Timer();
//            timer.schedule(task, ONE_MINUTE, interval);
//        }
//        public void stop(){
//            if(mMeter != null) {
//                mMeter.stop();
//                mMeter = null;
//            }
//            if(timer != null) {
//                timer.cancel();
//                timer = null;
//            }
//        }
//    }
//
//    TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
////            try {
////                Thread.sleep(backoff);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//            mMeter = null;
////            Random random = new Random();
////            backoff = random.nextInt(ONE_MINUTE - 1) + backoff;
//            System.gc();
//            mMeter = new SoundMeter(mMeterHandler, calibration, duration);
//            new Thread(mMeter).start();
//        }
//    };

//    class InfiniteMonitoringService implements Runnable {
//        boolean flag = true;
//        @Override
//        public void run() {
//            while(flag) {
//                try {
//                    Thread.sleep(interval);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if(flag) {
//                    mMeter = null;
//                    System.gc();
//                    Random random = new Random();
//                    backoff = (random.nextInt(Integer.SIZE - 1) % ONE_MINUTE) * 4 + ONE_MINUTE;
//                    mMeter = new SoundMeter(mMeterHandler, calibration, duration);
//                    new Thread(mMeter).start();
//                }
//                try {
//                    Thread.sleep(backoff);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        public void stop(){
//            flag = false;
//            if(mMeter != null) {
//                mMeter.stop();
//            }
//        }
//    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i("SoundMap", "GoogleApiClient connected");
        int permissionCheck = ContextCompat.checkSelfPermission(Audient.this,
                Manifest.permission.WRITE_CALENDAR);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(2*ONE_MINUTE); // Update location every five minute
            mLocationRequest.setFastestInterval(ONE_MINUTE); // Update location every second
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        Location lastLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(lastLoc != null){
            lat = lastLoc.getLatitude();
            lon = lastLoc.getLongitude();
        } else {
            //lastLoc is null;
            System.out.println("Last location is null");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("SoundMap", "Location changed");
        int permissionCheck = ContextCompat.checkSelfPermission(Audient.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            lat = location.getLatitude();
            lon = location.getLongitude();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("SoundMap", "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("SoundMap", "GoogleApiClient connection has failed");
    }


    NotificationManager nManager;
    PendingIntent pendingIntent;
    Notification notification;
    private void notification(){
        nManager = (NotificationManager) getSystemService(Audient.this.NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.setClass(this, SoundMap.class);
        intent.putExtra("username", name);
        intent.putExtra("from", "Audient");
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // 通过Notification.Builder来创建通知，注意API Level
        // API16之后才支持
        notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.red)
                .setTicker("TickerText:" + "Map4loud is running in background.")
                .setContentTitle("Map4loud")
                .setContentText("Silent mode is running in background.")
                .setContentIntent(pendingIntent).build();
                //.setContentIntent(pendingIntent).setNumber(nSeq++).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL; // FLAG_AUTO_CANCEL表明当通知被用户点击时，通知将被清除。
        //nManager.notify(NOTIFICATION_FLAG, notification);// 步骤4：通过通知管理器来发起通知。如果id不同，则每click，在status哪里增加一个提示
    }

    private void cancelNotification(){
        nManager.cancel(NOTIFICATION_FLAG);
    }


    private void acquireWakeLock(){
        if (mWakeLock==null){
            PowerManager pm = (PowerManager)Audient.this.getSystemService(Audient.this.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, TAG);
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }

    private void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }


}
