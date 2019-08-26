package com.dragonforest.app.facedemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.dragonforest.app.facedemo.activity_server.FaceRecognizeUploadActivity;
import com.dragonforest.app.facedemo.activity_server.FaceRecognizeUploadActivity2;
import com.dragonforest.app.facedemo.activity_server.FaceRecognizeUploadActivityDayou;
import com.dragonforest.app.facedemo.activity_server.FaceRegisterUploadActivity;
import com.dragonforest.app.facedemo.activity_server.FaceRegisterUploadActivity2;
import com.dragonforest.app.facedemo.arcsoft.common.Constants;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    /**
     * 人脸引擎是否激活
     */
    boolean isEngineActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 激活引擎
        activeEngine();
    }

    public void onClick(View view) {
        if (!isEngineActive) {
            Toast.makeText(this, "引擎还未激活", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (view.getId()) {
            case R.id.btn_face_register:
                go(FaceRegisterActivity.class);
                break;
            case R.id.btn_face_recognize:
                go(FaceRecognizeActivity.class);
                break;
            case R.id.btn_face_register_and_recognize:
                go(RegisterAndRecognizeActivity.class);
                break;
            case R.id.btn_face_img_upload_recognize:
                go(FaceRecognizeUploadActivity.class);
                break;
            case R.id.btn_face_feature_upload_recognize:
                go(FaceRecognizeUploadActivity2.class);
                break;
            case R.id.btn_face_img_upload_register:
                go(FaceRegisterUploadActivity.class);
                break;
            case R.id.btn_face_feature_upload_register:
                go(FaceRegisterUploadActivity2.class);
                break;
            case R.id.btn_face_img_upload_dayou:
                go(FaceRecognizeUploadActivityDayou.class);
                break;
        }
    }

    private void go(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        startActivity(intent);
    }


    /**
     * 激活引擎
     */
    public void activeEngine() {
        final FaceEngine faceEngine = new FaceEngine();
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                int activeCode = faceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            isEngineActive = true;
                            Toast.makeText(MainActivity.this, "引擎激活成功", Toast.LENGTH_SHORT).show();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            isEngineActive = true;
                            Toast.makeText(MainActivity.this, "引擎已经激活", Toast.LENGTH_SHORT).show();
                        } else {
                            isEngineActive = false;
                            Toast.makeText(MainActivity.this, "引擎激活失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

}
