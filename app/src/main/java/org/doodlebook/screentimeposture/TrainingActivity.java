package org.doodlebook.screentimeposture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStats;

public class TrainingActivity extends AppCompatActivity {

    static TextView vStatus;
    MyUtil myUtil = new MyUtil(this);
    static SensorManager sensorManager;
    private static Sensor sensorAccelerometer, sensorMagnetic, sensorGyro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_training);

        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }


    public static StringBuilder data;
    static HashMap<Integer, Long> sensorPrevmillis = new HashMap<Integer, Long>();
    static HashMap<Integer, String> sensorTypeName = new HashMap<Integer, String>();
    static SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
    public static String PHONE_INFO = Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();

    static SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();  // 1: accelerator, 2: magnetic, 4: gyroscope, ??: light
            long curr_millis = System.currentTimeMillis();
            if (curr_millis - sensorPrevmillis.get(sensorType) < 1000)
                return;
            sensorPrevmillis.put(sensorType, curr_millis);

            float[] xyz = event.values;
            String record = MyUtil.posture_choice + "," + PHONE_INFO
                    + "," + curr_millis + "," + formatter.format(new Date())
                    + "," + sensorType + "," + sensorTypeName.get(sensorType)
                    + "," + xyz[0] + "," + xyz[1] + "," + xyz[2] + "\n";

            vStatus.setText(record);
            data.append(record);
        }
    };

    public void startMyService(View view) {
        MyService.scheduleRepeat(view.getContext());
    }
    public void stopMyService(View view) {
        MyService.cancelRepeat(view.getContext());
        sensorManager.unregisterListener(sensorEventListener);
        isSensorRegistered = false;
    }

    public static boolean isSensorRegistered = false;
    public static void getAppUsageAndSensorData() {
        if (! isSensorRegistered) {
            sensorManager.registerListener(sensorEventListener, sensorAccelerometer, 20000000);
            sensorManager.registerListener(sensorEventListener, sensorMagnetic, 20000000);
            sensorManager.registerListener(sensorEventListener, sensorGyro, 20000000);

            long prev_millis = System.currentTimeMillis();
            sensorPrevmillis.put(Sensor.TYPE_ACCELEROMETER, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_GYROSCOPE, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_MAGNETIC_FIELD, prev_millis);

            sensorTypeName.put(Sensor.TYPE_ACCELEROMETER, "ACC");
            sensorTypeName.put(Sensor.TYPE_GYROSCOPE, "GYRO");
            sensorTypeName.put(Sensor.TYPE_MAGNETIC_FIELD, "MAGNET");

            isSensorRegistered = true;
            data = new StringBuilder();
        } else {
            sensorManager.unregisterListener(sensorEventListener);
            isSensorRegistered = false;
            //saveData();
        }
    }

    public void saveData() {
        String task = "service";
        try {
            String fname;
            fname = "posture_" + task + ".csv";
            FileOutputStream fOut = openFileOutput(fname, Context.MODE_PRIVATE | Context.MODE_APPEND);
            fOut.write(data.toString().getBytes());
            fOut.close();
            Log.i("successful saving: ", "");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("failed to save data", "");
        }
/*
        Boolean saveToWebServer = true;
//        Boolean saveToWebServer = false;
        if (saveToWebServer) {
            String data_type = "posture_xyz";
            myUtil.sendDataToWebServer(task, data_type, data.toString(),
                    new MyUtil.VolleyCallback() {
                        @Override
                        public void onSuccess(String result) {
                            vStatus.setText(result);
                        }

                        public void onError(String result) {
                            vStatus.setText("Error: " + result);
                        }
                    });
        }
        */
    }



    public void clickAppRecentTask(View view) {
        vStatus.setText(getAppUsageStats());
    }

    private String getAppUsageStats() {
        UsageStatsManager lUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> lUsageStatsList =
                lUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1),
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

        StringBuilder lStringBuilder = new StringBuilder();

        long currMillis = System.currentTimeMillis();
        for (UsageStats lUsageStats : lUsageStatsList) {
            long lastTimeMillis = lUsageStats.getLastTimeUsed();
            if (currMillis-lastTimeMillis < 120*1000) {
                lStringBuilder.append(lUsageStats.getPackageName());
                lStringBuilder.append("|");
                lStringBuilder.append(myUtil.millisToTimeString(lastTimeMillis));
                lStringBuilder.append("|");
                lStringBuilder.append(lUsageStats.getTotalTimeInForeground() / 1000);
                lStringBuilder.append("\n");
            }
        }
        return lStringBuilder.toString();
    }

}