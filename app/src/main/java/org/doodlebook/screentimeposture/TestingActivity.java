package org.doodlebook.screentimeposture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class TestingActivity extends AppCompatActivity {

    MyUtil myUtil = new MyUtil(this);;
    static TextView vStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_testing);
        //myUtil = new MyUtil(this);
        vStatus = (TextView) findViewById(org.doodlebook.screentimeposture.R.id.textViewGuess);
    }

    public void guessWhichRoom(View view) {
        String task = "test";
        String data = myUtil.scanWifi(task);
        myUtil.sendDataToWebServer(task, "wifi_signal", data,
            new MyUtil.VolleyCallback() {
                @Override
                public void onSuccess(String result){
                    vStatus.setText(result);
                }
                public void onError(String result){
                    vStatus.setText("Error: " + result);
                }
            });

    }

}