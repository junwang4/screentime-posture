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
import android.util.Log;

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

    static UsageStatsManager usageStatsManager;
    static SensorManager sensorManager;
    static Sensor sensorAccelerometer, sensorMagnetic, sensorGyro;

    static HashMap<Integer, Long> sensorPrevmillis = new HashMap<Integer, Long>();
    static HashMap<Integer, String> sensorTypeName = new HashMap<Integer, String>();
    static boolean isSensorRegistered;
    static StringBuilder data;
    static long listenCount = 0;
    static long serviceRunCount = 0;

    static RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_main);
        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public static void setupResourceAndServices() {
        Context c = MyApp.getContext();
        sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        usageStatsManager = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        requestQueue = Volley.newRequestQueue(c);
        //vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);
    }

    public void startMyService(View view) {
        myService.scheduleRepeat(view.getContext());
    }

    public void stopMyService(View view) {
        myService.cancelRepeat(view.getContext());
    }

    public static void unRegisterSensorListener() {
        sensorManager.unregisterListener(sensorEventListener);
    }
    public static void registerSensorListener() {
        sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //MainActivity.vStatus.setText("registerSensorListener " + MyUtil.formatter.format(new Date()));

        long prev_millis = System.currentTimeMillis();
        sensorPrevmillis.put(Sensor.TYPE_ACCELEROMETER, prev_millis);
        sensorPrevmillis.put(Sensor.TYPE_GYROSCOPE, prev_millis);
        sensorPrevmillis.put(Sensor.TYPE_MAGNETIC_FIELD, prev_millis);
        sensorTypeName.put(Sensor.TYPE_ACCELEROMETER, "ACC");
        sensorTypeName.put(Sensor.TYPE_GYROSCOPE, "GYRO");
        sensorTypeName.put(Sensor.TYPE_MAGNETIC_FIELD, "MAGNET");

        data = new StringBuilder();
        listenCount = 0;
        serviceRunCount = 0;
        isSensorRegistered = true;
    }


    public static SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();  // 1: accelerator, 2: magnetic, 4: gyroscope, ??: light
            long curr_millis = System.currentTimeMillis();
            if (curr_millis - sensorPrevmillis.get(sensorType) < 2000)
                return;
            sensorPrevmillis.put(sensorType, curr_millis);

            float[] xyz = event.values;
            String record = MyUtil.posture_choice + "," + MyUtil.PHONE_INFO
                    + "," + curr_millis + "," + MyUtil.formatter.format(new Date())
                    + "," + sensorType + "," + sensorTypeName.get(sensorType)
                    + "," + xyz[0] + "," + xyz[1] + "," + xyz[2] + "\n";

            //MainActivity.vStatus.setText(++listenCount + "   \n" + record);
            Log.i("info", ++listenCount + "   \n" + record);
            data.append(record);
            if (listenCount % 5 == 0)
                saveData();
        }
    };


    static void saveData() {
        sendDataToWebServer("service", "posture_xyz", data.toString(),
            new MyUtil.VolleyCallback() {
                @Override
                public void onSuccess(String result){
                    //MainActivity.vStatus.setText(result);
                    Log.i("from server", result);
                }
                public void onError(String result){
                    //MainActivity.vStatus.setText("Error: " + result);
                }
            });
    }

    static void sendDataToWebServer(String task, String data_type, String data, final MyUtil.VolleyCallback callback) {
        final String data_type1 = data_type;
        final String data1 = data;
        final String task1 = task;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, MyUtil.SERVER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError(error.toString());
                        Log.e("Error", error.toString());
                    }
                })
        {
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("task", task1);
                params.put("type", data_type1);
                params.put("data", data1);
                return params;
            }
        };

        //RequestQueue requestQueue = Volley.newRequestQueue(this); // see onCreate() : Volley.newRequestQueue(getApplicationContext())
        requestQueue.add(stringRequest);
    }




    public void clickSituationBed(View view) {
        MainActivity.vStatus.setText("test...");

        UsageStatsManager lUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> lUsageStatsList =
                lUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(60),
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(60));

        StringBuilder lStringBuilder = new StringBuilder();

        long currMillis = System.currentTimeMillis();
        for (UsageStats lUsageStats : lUsageStatsList) {
            long lastTimeMillis = lUsageStats.getLastTimeUsed();
            if (currMillis-lastTimeMillis < 2*1000) {
            lStringBuilder.append(lUsageStats.getPackageName());
            lStringBuilder.append("|");
            lStringBuilder.append(MyUtil.millisToTimeString(lastTimeMillis));
            lStringBuilder.append("|");
            lStringBuilder.append(lUsageStats.getTotalTimeInForeground() / 1000);
            lStringBuilder.append("\n");
            }
        }
        MainActivity.vStatus.setText(lStringBuilder.toString());
    }

}
