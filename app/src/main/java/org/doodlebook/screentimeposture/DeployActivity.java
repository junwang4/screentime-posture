package org.doodlebook.screentimeposture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class DeployActivity extends AppCompatActivity {
    private static final int GPS_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_deploy);
    }

    public void startDeploy(View view) {
        MyService.scheduleRepeat(view.getContext());
    }

    public void stopTracking(View view) {
        MyService.cancelRepeat(view.getContext());
    }
}
