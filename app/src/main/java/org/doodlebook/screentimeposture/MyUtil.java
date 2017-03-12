package org.doodlebook.screentimeposture;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyUtil {

    public static String PHONE_INFO = Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();

    public static String posture_choice = "For test";
    public static final String SERVER_URL = "http://192.168.1.178:5000/api";
    //public static final String SERVER_URL = "http://128.230.146.72:8080/api";  // textmining



    private Context context;

    public MyUtil(Context context) {
        this.context = context;
    }

    public String getSensorXYZ(String task) {
        return("");
    }


    public String scanWifi(String task) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.startScan();
        List<ScanResult> wifiScanList = wifi.getScanResults();

        int numOfWifi = wifiScanList.size();

        String data = "";
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();
        Date date = new Date(ts);
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
        String ttt = formatter.format(date);

        String room = "";
        if (task == "train")
            room = posture_choice;;

        String forUser = "";
        for (ScanResult scanResult : wifiScanList) {
            String ssid = scanResult.SSID;
            String bssid = scanResult.BSSID;
            int rss = 2 * (100 + scanResult.level);
            String row = room + "," + ttt + "," + ts + "," + bssid + "," + ssid + "," + rss + "\n";
            data += row;
            forUser += ssid + " : " + rss + "\n";
        }
        Log.i("data", ttt + " " + room);

        try {
            String fname;
            fname = "wifi_" + task + ".csv";
            FileOutputStream fOut = context.openFileOutput(fname, Context.MODE_PRIVATE | Context.MODE_APPEND);
            fOut.write(data.getBytes());
            fOut.close();
            Log.i("successful saving: ", "");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("failed to save data", "");
        }
        //return(forUser);
        return(data);
    }


    public interface VolleyCallback {
        void onSuccess(String result);
        void onError(String result);
    }

    public void sendDataToWebServer(String task, String data_type, String data, final VolleyCallback callback) {
        final String data_type1 = data_type;
        final String data1 = data;
        final String task1 = task;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, SERVER_URL,
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

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }

    SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm:ss a");
    public String millisToTimeString(long millis) {
        Timestamp timestamp = new Timestamp(millis);
        long ts = timestamp.getTime();
        Date date = new Date(ts);
        return formatter.format(date);
    }


    }