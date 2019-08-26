package com.dragonforest.app.facedemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };

    private int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (requestPers()) {
            // 进入main
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }

    private boolean requestPers() {
        List<String> needRequirePers = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                needRequirePers.add(permission);
            }
        }
        if (needRequirePers.size() == 0) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, needRequirePers.toArray(new String[needRequirePers.size()]), REQUEST_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        }
        // 进入main
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
