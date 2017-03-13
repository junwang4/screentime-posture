package org.doodlebook.screentimeposture;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import android.content.Intent;
import android.widget.TextView;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    static TextView vStatus;
    MyService myService = new MyService();

    public static SensorManager sensorManager;
    public static Sensor sensorAccelerometer, sensorMagnetic, sensorGyro;
    public static boolean isSensorRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_main);
        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void startMyService(View view) {
        isSensorRegistered = false;
        myService.scheduleRepeat(view.getContext());
    }

    public void stopMyService(View view) {
        myService.cancelRepeat(view.getContext());
        sensorManager.unregisterListener(sensorEventListener);
        isSensorRegistered = false;
    }


    static HashMap<Integer, Long> sensorPrevmillis = new HashMap<Integer, Long>();
    static HashMap<Integer, String> sensorTypeName = new HashMap<Integer, String>();

    public static SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();  // 1: accelerator, 2: magnetic, 4: gyroscope, ??: light
            long curr_millis = System.currentTimeMillis();
            if (curr_millis - sensorPrevmillis.get(sensorType) < 1000)
                return;
            sensorPrevmillis.put(sensorType, curr_millis);

            float[] xyz = event.values;
            String record = MyUtil.posture_choice + "," + MyUtil.PHONE_INFO
                    + "," + curr_millis + "," + MyUtil.formatter.format(new Date())
                    + "," + sensorType + "," + sensorTypeName.get(sensorType)
                    + "," + xyz[0] + "," + xyz[1] + "," + xyz[2] + "\n";

            MainActivity.vStatus.setText(record);
            //data.append(record);
        }
    };


    public static void getAppUsageAndSensorData() {
        if (! isSensorRegistered) {
            isSensorRegistered = true;

            MainActivity.sensorManager.registerListener(sensorEventListener, MainActivity.sensorAccelerometer, 20000000);
            MainActivity.sensorManager.registerListener(sensorEventListener, MainActivity.sensorMagnetic, 20000000);
            MainActivity.sensorManager.registerListener(sensorEventListener, MainActivity.sensorGyro, 20000000);

            long prev_millis = System.currentTimeMillis();
            sensorPrevmillis.put(Sensor.TYPE_ACCELEROMETER, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_GYROSCOPE, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_MAGNETIC_FIELD, prev_millis);

            sensorTypeName.put(Sensor.TYPE_ACCELEROMETER, "ACC");
            sensorTypeName.put(Sensor.TYPE_GYROSCOPE, "GYRO");
            sensorTypeName.put(Sensor.TYPE_MAGNETIC_FIELD, "MAGNET");

            //data = new StringBuilder();
        } else {
            isSensorRegistered = false;
            MainActivity.sensorManager.unregisterListener(sensorEventListener);
            //saveData();
        }
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
                lStringBuilder.append(MyUtil.millisToTimeString(lastTimeMillis));
                lStringBuilder.append("|");
                lStringBuilder.append(lUsageStats.getTotalTimeInForeground() / 1000);
                lStringBuilder.append("\n");
            }
        }
        return lStringBuilder.toString();
    }


}
