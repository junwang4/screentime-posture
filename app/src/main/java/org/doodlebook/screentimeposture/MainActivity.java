package org.doodlebook.screentimeposture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    public static String mainTask = "activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.doodlebook.screentimeposture.R.layout.activity_main);
    }

    public void goTrain(View view) {
        Intent intent = new Intent(this, TrainingActivity.class);
        startActivity(intent);
    }
    public void goTest(View view) {
        Intent intent = new Intent(this, TestingActivity.class);
        startActivity(intent);
    }
    public void goDeploy(View view) {
        Intent intent = new Intent(this, DeployActivity.class);
        startActivity(intent);
    }


}
