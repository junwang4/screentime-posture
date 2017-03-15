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

public class MyService extends GcmTaskService {

    private static final String TAG = MyService.class.getSimpleName();
    public static final String GCM_REPEAT_TAG = "repeat|[7200,1800]";

    @Override
    public void onInitializeTasks() {
        // called when app is updated to a new version, reinstalled etc.
        // you have to schedule your repeating tasks again
        super.onInitializeTasks();
    }


    public void scheduleRepeat(Context context) {

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
                    Toast.makeText(MyService.this, "onRunTask executed: ", Toast.LENGTH_LONG).show();

                    getAppUsageStats();

                    //String data = myUtil.scanWifi("train");

                }
            });
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }


    public void cancelRepeat(Context context) {
        Toast.makeText(context, "Stop the background service", Toast.LENGTH_LONG).show();
        MainActivity.unRegisterSensorListener();
        GcmNetworkManager.getInstance(context).cancelTask(GCM_REPEAT_TAG, MyService.class);
    }


    private String getAppUsageStats() {
        MainActivity.setupResourceAndServices();

        List<UsageStats> usageStatsList =
                MainActivity.usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5),
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        StringBuilder stringBuilder = new StringBuilder();

        long currMillis = System.currentTimeMillis();
        for (UsageStats usageStats : usageStatsList) {
            long lastTimeMillis = usageStats.getLastTimeUsed();
            if (currMillis-lastTimeMillis < 20*1000) {
                stringBuilder.append(usageStats.getPackageName());
                stringBuilder.append("|");
                stringBuilder.append(MyUtil.millisToTimeString(lastTimeMillis));
                stringBuilder.append("|");
                stringBuilder.append(usageStats.getTotalTimeInForeground() / 1000);
                stringBuilder.append("\n");

                //if ( usageStats.getPackageName() == "com.android.chrome") myStartSensorListener();
            }
        }
        String usage = stringBuilder.toString();
        if (usage.length()>0) {
            Toast.makeText(MyService.this, usage, Toast.LENGTH_LONG).show();
            MainActivity.registerSensorListener();
        }
        else {
            MainActivity.unRegisterSensorListener();
            Toast.makeText(MyService.this,  "unregister listener", Toast.LENGTH_LONG).show();
            Log.i("info", "unregister listener");
        }

        return stringBuilder.toString();
    }


}
