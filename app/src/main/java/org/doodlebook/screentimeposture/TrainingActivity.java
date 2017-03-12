package org.doodlebook.screentimeposture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

public class TrainingActivity extends AppCompatActivity implements OnItemSelectedListener {

    static TextView vStatus;
    MyUtil myUtil = new MyUtil(this);
    SensorManager sensorManager;
    private Sensor sensorAccelerometer, sensorMagnetic, sensorGyro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_training);

        Spinner spinner = (Spinner) findViewById(org.doodlebook.screentimeposture.R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewStatus);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        MyUtil.posture_choice = parent.getItemAtPosition(position).toString();
        Toast.makeText(parent.getContext(), "Selected: " + MyUtil.posture_choice, Toast.LENGTH_LONG).show();
        vStatus.setText("");
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }


    public static StringBuilder data;
    HashMap<Integer, Long> sensorPrevmillis = new HashMap<Integer, Long>();
    HashMap<Integer, String> sensorTypeName = new HashMap<Integer, String>();

    SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");

    SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();  // 1: accelerator, 2: magnetic, 4: gyroscope, ??: light
            long curr_millis = System.currentTimeMillis();
            if (curr_millis - sensorPrevmillis.get(sensorType) < 1000)
                return;
            sensorPrevmillis.put(sensorType, curr_millis);

            float[] xyz = event.values;
            String record = MyUtil.posture_choice + "," + myUtil.PHONE_INFO
                    + "," + curr_millis + "," + formatter.format(new Date())
                    + "," + sensorType + "," + sensorTypeName.get(sensorType)
                    + "," + xyz[0] + "," + xyz[1] + "," + xyz[2] + "\n";

            vStatus.setText(record);
            data.append(record);
        }
    };


    public void startObtainPostureData(View view) {
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

        //sensorManager.registerListener(sel, (Sensor) sensorList.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        // see: http://stackoverflow.com/questions/16783325/android-user-defined-delay-is-used-in-registerlistener-not-working-why
        //     public static final int SENSOR_DELAY_FASTEST = 0;
        //     public static final int SENSOR_DELAY_GAME = 1;
        //     public static final int SENSOR_DELAY_UI = 2;
        //     public static final int SENSOR_DELAY_NORMAL = 3; // 200000ms rate (default) suitable for screen orientation changes
        // So as long as your value is not 0,1,3 then you will be ok and yours should be used.

        data = new StringBuilder();
    }

    public void stopObtainData(View view) {
        sensorManager.unregisterListener(sensorEventListener);
    }

    public void saveData(View view) {
        String task = "train";
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
    }


    public void learnPattern(View view) {

        myUtil.sendDataToWebServer("", "train_model", "",
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