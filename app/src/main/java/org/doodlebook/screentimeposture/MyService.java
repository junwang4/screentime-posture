package org.doodlebook.screentimeposture;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyService extends GcmTaskService implements SensorEventListener {

    private static final String TAG = MyService.class.getSimpleName();
    public static final String GCM_REPEAT_TAG = "repeat|[7200,1800]";

    static SensorManager sensorManager;
    static Sensor sensorAccelerometer, sensorMagnetic, sensorGyro;
    static boolean isSensorRegistered;
    static StringBuilder data;
    static long listenCount = 0;
    static long serviceRunCount = 0;


    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    public void onSensorChanged(SensorEvent event) {
        if (!isSensorRegistered) {
            return;
        }

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

        MainActivity.vStatus.setText(++listenCount + "   \n" + record);
        data.append(record);

        if (listenCount % 10 == 0) {
            saveData();
            data = new StringBuilder();
        }
    }


    @Override
    public void onInitializeTasks() {
        // called when app is updated to a new version, reinstalled etc.
        // you have to schedule your repeating tasks again
        super.onInitializeTasks();
        myStartSensorListener();
    }


    void myStartSensorListener() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        MainActivity.vStatus.setText("myStart " + MyUtil.formatter.format(new Date()) + "   register listener  " + listenCount);

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


    public void scheduleRepeat(Context context) {
        //in this method, single Repeating task is scheduled (the target service that will be called is MyTaskService.class)
        try {

            PeriodicTask periodic = new PeriodicTask.Builder()
                    //specify target service - must extend GcmTaskService
                    .setService(MyService.class)
                    .setPeriod(30)
                    //specify how much earlier the task can be executed (in seconds)
                    .setFlex(10)
                    //tag that is unique to this task (can be used to cancel task)
                    .setTag(GCM_REPEAT_TAG)
                    //whether the task persists after device reboot
                    .setPersisted(true)
                    //if another task with same tag is already scheduled, replace it with this task
                    .setUpdateCurrent(true)
                    //set required network state, this line is optional
                    .setRequiredNetwork(Task.NETWORK_STATE_ANY)
                    //request that charging must be connected, this line is optional
                    .setRequiresCharging(false)
                    .build();
            GcmNetworkManager.getInstance(context).schedule(periodic);
            Log.v(TAG, "Start the background service: repeating task scheduled");
            Toast.makeText(context, "Start the background service", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "scheduling failed");
            e.printStackTrace();
        }
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        //do some stuff (mostly network) - executed in background thread (async)
        //obtain your data
        Bundle extras = taskParams.getExtras();

        Handler h = new Handler(getMainLooper());
        Log.v(TAG, "onRunTask");
        if(taskParams.getTag().equals(GCM_REPEAT_TAG)) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    //String data = myUtil.scanWifi("train");

                    //getAppUsageAndSensorData();
                    isSensorRegistered = !isSensorRegistered;
                    /*if (!isSensorRegistered) {
                        saveData();
                        data = new StringBuilder();
                    }*/


                    Toast.makeText(MyService.this, "onRunTask executed: " + isSensorRegistered, Toast.LENGTH_LONG).show();
                }
            });
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }


    public void cancelRepeat(Context context) {
        sensorManager.unregisterListener(this);
        isSensorRegistered = false;
        MainActivity.vStatus.setText(MyUtil.formatter.format(new Date()) + " : suppose to unregister listener");

        Toast.makeText(context, "Stop the background service", Toast.LENGTH_LONG).show();

        GcmNetworkManager.getInstance(context).cancelTask(GCM_REPEAT_TAG, MyService.class);
    }


    HashMap<Integer, Long> sensorPrevmillis = new HashMap<Integer, Long>();
    HashMap<Integer, String> sensorTypeName = new HashMap<Integer, String>();


    void saveData() {
        sendDataToWebServer_temp("service", "posture_xyz", data.toString(),
            new MyUtil.VolleyCallback() {
                @Override
                public void onSuccess(String result){
                    MainActivity.vStatus.setText(result);
                }
                public void onError(String result){
                    MainActivity.vStatus.setText("Error: " + result);
                }
            });
    }

    void sendDataToWebServer_temp(String task, String data_type, String data, final MyUtil.VolleyCallback callback) {
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
                        //Toast.makeText(thisActivity, error.toString(), Toast.LENGTH_LONG).show();
                        callback.onError(error.toString());
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

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }



}
