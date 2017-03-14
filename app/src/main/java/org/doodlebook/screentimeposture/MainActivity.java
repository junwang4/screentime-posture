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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    static TextView vStatus;
    MyService myService = new MyService();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_main);
        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);
    }

    public void startMyService(View view) {
        myService.scheduleRepeat(view.getContext());
    }

    public void stopMyService(View view) {
        myService.cancelRepeat(view.getContext());
    }



    private String getAppUsageStatsTest() {
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

    /*
    void getAppUsageAndSensorData() {
        if (! isSensorRegistered) {
            isSensorRegistered = true;

//            sensorManager.registerListener(sensorEventListener, sensorAccelerometer, 20000000);
            //sensorManager.registerListener(sensorEventListener, sensorMagnetic, 20000000);
            //sensorManager.registerListener(sensorEventListener, sensorGyro, 20000000);
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            long prev_millis = System.currentTimeMillis();
            sensorPrevmillis.put(Sensor.TYPE_ACCELEROMETER, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_GYROSCOPE, prev_millis);
            sensorPrevmillis.put(Sensor.TYPE_MAGNETIC_FIELD, prev_millis);

            sensorTypeName.put(Sensor.TYPE_ACCELEROMETER, "ACC");
            sensorTypeName.put(Sensor.TYPE_GYROSCOPE, "GYRO");
            sensorTypeName.put(Sensor.TYPE_MAGNETIC_FIELD, "MAGNET");

            data = new StringBuilder();
            MainActivity.vStatus.setText(MyUtil.formatter.format(new Date()) + "register listener");
        } else {
            isSensorRegistered = false;
            sensorManager.unregisterListener(this);
            MainActivity.vStatus.setText(MyUtil.formatter.format(new Date()) + "suppose to unregister listener");
            saveData();
            data = new StringBuilder();
        }
    }
*/

        /*
    SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (!isSensorRegistered) {
                sensorManager.unregisterListener(sensorEventListener); // in case unregisterListener not works
                return;
            }

            int sensorType = event.sensor.getType();  // 1: accelerator, 2: magnetic, 4: gyroscope, ??: light
            long curr_millis = System.currentTimeMillis();
            if (curr_millis - sensorPrevmillis.get(sensorType) < 5000)
                return;
            sensorPrevmillis.put(sensorType, curr_millis);

            float[] xyz = event.values;
            String record = MyUtil.posture_choice + "," + MyUtil.PHONE_INFO
                    + "," + curr_millis + "," + MyUtil.formatter.format(new Date())
                    + "," + sensorType + "," + sensorTypeName.get(sensorType)
                    + "," + xyz[0] + "," + xyz[1] + "," + xyz[2] + "\n";

            MainActivity.vStatus.setText(record);
            data.append(record);
        }
    };
*/

