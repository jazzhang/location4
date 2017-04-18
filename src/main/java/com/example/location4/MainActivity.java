package com.example.location4;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.location4.R.id.latitude_value;

public class MainActivity extends AppCompatActivity {

    public LocationManager locationManager;
    public ListView listView;
    public long exitTime = 0;     // 两秒内按返回键两次退出程序
    public TextView latitude;
    public TextView longtitude;
    public TextView altitude;
    public TextView TTFF;
    public TextView inused_total;
    public boolean isrunning = false;
    public String Start_mode = "Cold Start";

    public ListView SVinfo_listView;
    public ArrayList<Map<String, Object>> svData = new ArrayList<Map<String, Object>>();
    public SimpleAdapter adapter;

    public FileOutputStream fw = null;
    public String logpath = "/sdcard/gps_nmea_log";
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
    private int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;

    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    private static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";

    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS是否正常启动
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please Enable GPS in Setting page", Toast.LENGTH_SHORT).show();
            // 返回开启GPS导航设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            //has permission, do operation directly
            //ContactsUtils.readPhoneContacts(this);
            Log.i("DEBUG_TAG", "user has the permission already!");
            System.out.println("======>>>>>>> have permission");
        } else {
            System.out.println("======>>>>>>> have no permission");

            //do not have permission
            Log.i("DEBUG_TAG", "user do not have this permission!");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                System.out.println("we should explain why we need this permission!");
            } else {

                // No explanation needed, we can request the permission.
                System.out.println("==request the permission==");

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //has permission, do operation directly
            //ContactsUtils.readPhoneContacts(this);
            Log.i("DEBUG_TAG", "user has the permission already!");
            System.out.println("======>>>>>>> have permission");
        } else {
            System.out.println("======>>>>>>> have no permission");

            //do not have permission
            Log.i("DEBUG_TAG", "user do not have this permission!");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                System.out.println("we should explain why we need this permission!");
            } else {

                // No explanation needed, we can request the permission.
                System.out.println("==request the permission==");

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }

        latitude = (TextView) findViewById(latitude_value);
        longtitude = (TextView) findViewById(R.id.longtitude_value);
        altitude = (TextView) findViewById(R.id.altitude_value);
        TTFF = (TextView) findViewById(R.id.TTFF_value);
        inused_total = (TextView) findViewById(R.id.inused_total);

        SetGnssstatus();
        // set the list view
        SVinfo_listView = (ListView) findViewById(R.id.listview);
        Map<String, Object> item = new HashMap<String, Object>();

        item.put("prn", "PRN");
        item.put("snr", "SNR");
        item.put("el", "EL");
        item.put("az", "AZ");
        item.put("flag", R.mipmap.ic_notification);
        item.put("used_in_fix", R.mipmap.ic_selected);
        svData.add(item);

        adapter = new SimpleAdapter(
                this,
                svData,
                R.layout.list_item,
                new String[]{"prn", "snr", "el", "az", "flag", "used_in_fix"},
                new int[]{R.id.prn, R.id.snr, R.id.el, R.id.az, R.id.flag, R.id.used_in_fix});
        SVinfo_listView.setAdapter(adapter);


    }


    //private class TestLocationListener implements LocationListener {
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //当位置发生改变时调用
            System.out.println("===>" + location.getLongitude() + "|" + location.getLatitude());
            latitude.setText(Double.toString(location.getLatitude()));
            longtitude.setText(Double.toString(location.getLongitude()));
            altitude.setText(Double.toString(location.getAltitude()));
            //textView_position.setText("Long :" + location.getLongitude() + "\n" + "Lat  :" + location.getLatitude()
            //        + "\n" + "Alti :" + location.getAltitude());
        }

        @Override
        public void onProviderDisabled(String providmer) {
            //当适配器禁用时调用
        }

        @Override
        public void onProviderEnabled(String provider) {
            //当适配器有效时调用
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {
        }

    };
    /*this interface is new from level24 */
    private GnssStatus.Callback mgnsslistner = new GnssStatus.Callback() {

        @Override
        public void onFirstFix(int ttffMillis) {
            System.out.println("==> ttf is " + (double) ttffMillis / 1000);
            Double t1 = (double) ttffMillis / 1000;
            TTFF.setText(Double.toString(t1));
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            if (status != null) {
                svData.clear();
                Map<String, Object> item1 = new HashMap<String, Object>();

                item1.put("prn", "SvID");
                item1.put("snr", "CN0");
                item1.put("el", "EL");
                item1.put("az", "AZ");
                item1.put("flag", R.mipmap.ic_notification);
                item1.put("used_in_fix", R.mipmap.ic_selected);

                svData.add(item1);

                int SV_total = status.getSatelliteCount();
                int in_used = 0;
                for (int i = 0; i < SV_total; i++) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    //item.put("prn", Integer.toString(status.getSvid(i)));   //prn
                    item.put("snr", Float.toString(status.getCn0DbHz(i)));
                    item.put("el", String.format("%.2f", status.getElevationDegrees(i)) + "°");
                    item.put("az", String.format("%.2f", status.getAzimuthDegrees(i)) + "°");
                    //item.put("used_in_fix", Boolean.toString(status.usedInFix(i)));
                    boolean used1 = status.usedInFix(i);
                    if (used1) {
                        item.put("used_in_fix", R.mipmap.ic_selected);
                        in_used++;
                    } else {
                        item.put("used_in_fix", R.mipmap.ic_infoloc_close);
                    }

                    int temp = status.getConstellationType(i);
                    if (temp == 0) {
                        //item.put("flag", "unknow");
                        item.put("flag", R.mipmap.satellite_xx);
                        item.put("prn", Integer.toString(status.getSvid(i)));
                    } else if (temp == 1) {
                        //item.put("flag", "Gps");
                        item.put("flag", R.mipmap.satellite_us);
                        item.put("prn", Integer.toString(status.getSvid(i)));   //prn
                    } else if (temp == 2) {
                        // item.put("flag", "Sbas");
                        item.put("flag", R.mipmap.satellite_sbas);
                        item.put("prn", Integer.toString(status.getSvid(i)));
                        //between 120~138
                    } else if (temp == 3) {
                        //item.put("flag", "Glo");
                        item.put("flag", R.mipmap.satellite_ru);
                        item.put("prn", Integer.toString(status.getSvid(i) + 64));   //prn
                        //svid between 64~88
                    } else if (temp == 4) {
                        //item.put("flag", "Qzss");
                        item.put("flag", R.mipmap.satellite_jp);
                        item.put("prn", Integer.toString(status.getSvid(i)));
                    } else if (temp == 5) {
                        //item.put("flag", "Bds");
                        item.put("flag", R.mipmap.satellite_cn);
                        item.put("prn", Integer.toString(status.getSvid(i) + 200));   //prn
                        //svid between 201~237
                    } else if (temp == 6) {//GAL
                        item.put("flag", R.mipmap.satellite_eu);
                        item.put("prn", Integer.toString(status.getSvid(i) + 300));   //prn
                        //svid betwwen 301~336
                    } else {
                        item.put("flag", R.mipmap.satellite_xx);
                        item.put("prn", Integer.toString(status.getSvid(i)));   //prn

                    }
                    svData.add(item);
                }
                adapter.notifyDataSetChanged();
                //update inused/total in UI
                inused_total.setText(Integer.toString(in_used) + "/" + Integer.toString(SV_total));

            }
        }
    };

    private void SetGnssstatus() {
        this.latitude.setText("0");
        this.longtitude.setText("0");
        this.altitude.setText("0");
        this.TTFF.setText("0");
        this.inused_total.setText("0/0");
    }

    private GnssMeasurementsEvent.Callback eventlister = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            System.out.println("===> in event callback function feature");
            Iterator<GnssMeasurement> iterator = eventArgs.getMeasurements()
                    .iterator();
            while (iterator.hasNext()) {
                GnssMeasurement gmeasurement = iterator.next();
                System.out.println("===>sv= " + gmeasurement.getSvid() + "|CN0=" + gmeasurement.getCn0DbHz());
            }
            System.out.println("====================================================");
        }

        @Override
        public void onStatusChanged(int status) {
            System.out.println("====>event status changed");
            super.onStatusChanged(status);
        }
    };


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;

        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start:
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                locationManager.registerGnssStatusCallback(mgnsslistner);
                locationManager.registerGnssMeasurementsCallback(eventlister);
                System.out.println("====>Start\n");
                SetGnssstatus();
                start_log();
                // Here we might start a background refresh task
                return true;

            case R.id.stop:
                // Here we might call LocationManager.requestLocationUpdates()
                locationManager.unregisterGnssMeasurementsCallback(eventlister);
                locationManager.unregisterGnssStatusCallback(mgnsslistner);
                locationManager.removeUpdates(locationListener);
                System.out.println("====>end\n");
                stop_log();
                return true;

            case R.id.clear_aiding_data:
                System.out.println("====>need to implement here\n");
                Bundle extras = new Bundle();
                extras.putBoolean("all", true);
                if (locationManager.sendExtraCommand("gps", "delete_aiding_data", extras)) {
                    System.out.println("==>Delete aiding data");
                    Toast.makeText(getApplicationContext(), "Dele aid data", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Dele aid Failed", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.about:
                Toast.makeText(getApplicationContext(), "Released under GPL lisence,the phone should be Android api 24 or later ", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void stop_log() {
        if (fw != null)
            try {
                fw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    public void start_log() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String nowTime = format.format(new Date());
        File logfilepath;
        logfilepath = new File(logpath);
        logfilepath = new File(logpath);
        if (!logfilepath.exists()) {
            logfilepath.mkdirs();
        }
        File nmea_file = new File(logpath, "nmea_"
                + nowTime + ".txt"); // /create log file
        if (!nmea_file.exists()) {
            try {
                nmea_file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            fw = new FileOutputStream(nmea_file);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
