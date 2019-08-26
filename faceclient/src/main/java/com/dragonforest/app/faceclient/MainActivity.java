package com.dragonforest.app.faceclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.dragonforest.app.faceclient.arcsoft.common.Constants;
import com.dragonforest.app.faceclient.config.Configuration;
import com.dragonforest.app.faceclient.dialog.ChangeServerDialog;
import com.dragonforest.app.faceclient.util.ArcUtil;
import com.dragonforest.app.faceclient.util.SharedPreferenceUtil;
import com.faceclient.sdk.ArcFaceSDK;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    ChangeServerDialog changeServerDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 激活引擎
        ArcUtil.getInstance().activeEngine(this);
        // 初始化引擎
        ArcUtil.getInstance().initEngine(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_face_register:
                go(FaceRegisterActivity.class);
                break;
            case R.id.btn_face_recognize:
                go(FaceRecognizeActivity.class);
                break;
            case R.id.btn_face_auto_recognize:
                go(FaceAutoRecognizeActivity.class);
                break;
        }
    }

    private void go(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_server:
                showChangeServerDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChangeServerDialog() {
        if (changeServerDialog == null) {
            changeServerDialog = new ChangeServerDialog(this);
            String savedIp = SharedPreferenceUtil.get(this, SharedPreferenceUtil.KEY_FACESERVER_IP);
            String savedPort = SharedPreferenceUtil.get(this, SharedPreferenceUtil.KEY_FACESERVER_PORT);
            if (savedIp == null || "".equals(savedIp)) {
                savedIp = Configuration.FACESERVER_IP;
                savedPort = Configuration.FACESERVER_PORT;
            }
            changeServerDialog.setData(savedIp, savedPort);
            changeServerDialog.setOnChangeListener(new ChangeServerDialog.OnChangeListener() {
                @Override
                public void onCancel() {

                }

                @Override
                public void onChange(String ip, String port) {
                    // 修改当前server
                    ArcFaceSDK.getInstance().init(ip, port);
                    // 保存本地ip port
                    SharedPreferenceUtil.set(MainActivity.this, SharedPreferenceUtil.KEY_FACESERVER_IP, ip);
                    SharedPreferenceUtil.set(MainActivity.this, SharedPreferenceUtil.KEY_FACESERVER_PORT, port);
                    Toast.makeText(MainActivity.this, "切换server成功", Toast.LENGTH_SHORT).show();
                }
            });
        }
        changeServerDialog.show();
    }
}