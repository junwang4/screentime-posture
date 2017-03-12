package org.doodlebook.screentimeposture;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

public class MyService extends GcmTaskService {

    private static final String TAG = MyService.class.getSimpleName();

    public static final String GCM_ONEOFF_TAG = "oneoff|[0,0]";
    public static final String GCM_REPEAT_TAG = "repeat|[7200,1800]";
    MyUtil myUtil = new MyUtil(this);

    @Override
    public void onInitializeTasks() {
        //called when app is updated to a new version, reinstalled etc.
        //you have to schedule your repeating tasks again
        super.onInitializeTasks();
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
                    Toast.makeText(MyService.this, "onRunTask executed", Toast.LENGTH_LONG).show();
                    //String task = "service";
                    //String data = myUtil.scanWifi(task);
                    TrainingActivity.getAppUsageAndSensorData();
                    /*
                    myUtil.sendDataToWebServer(task, "posture_xyz", data,
                            new MyUtil.VolleyCallback() {
                                @Override
                                public void onSuccess(String result){
                                    //vStatus.setText(result);
                                }
                                public void onError(String result){
                                    //vStatus.setText("Error: " + result);
                                }
                            });
                            */

                }
            });
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }


    public static void scheduleRepeat(Context context) {
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
            Log.v(TAG, "repeating task scheduled");
            Toast.makeText(context, "Start the repeating track task", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "scheduling failed");
            e.printStackTrace();
        }
    }


    public static void cancelRepeat(Context context) {
        GcmNetworkManager
                .getInstance(context)
                .cancelTask(GCM_REPEAT_TAG, MyService.class);

        Toast.makeText(context, "Stop the repeating track task", Toast.LENGTH_LONG).show();
    }

}
