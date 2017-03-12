package org.doodlebook.screentimeposture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;

public class ScanService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int time = 0;
    Timer timer;
    TimerTask myTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timer = new Timer();
        myTask = new TimerTask() {
            @Override
            public void run() {
                scanWifi();
                //Log.i("in timer", "in timer ++++  "+ (time++));
            }
        };
        timer.schedule(myTask, 1000, 60*1000);

        Toast.makeText(this, "Start wifi-scan ...", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        super.onDestroy();
        Toast.makeText(this, "Stop Wifi-scan", Toast.LENGTH_LONG).show();
    }

    public void scanWifi() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.startScan();
        List<ScanResult> wifiScanList = wifi.getScanResults();

        int numOfWifi = wifiScanList.size();

        String data = "";
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();

        for (ScanResult scanResult : wifiScanList) {
            String ssid = scanResult.SSID;
            String bssid = scanResult.BSSID;
            int rss = 2 * (100 + scanResult.level);
            String row = "" + ts + "," + bssid + "," + ssid + "," + rss + "\n";
            data += row;
        }
        //Log.i("data", data);
        saveData(data);
    }

    private void saveData(String data) {
        try {
            String fname;
            fname = "wifi_signal.csv";
            fname = "wifi_3.csv";
            FileOutputStream fOut = openFileOutput(fname, Context.MODE_PRIVATE | Context.MODE_APPEND);
            fOut.write(data.getBytes());
            fOut.close();
            Log.i("successful saving data: ", "");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("failed to save data", "");
        }
    }

}
