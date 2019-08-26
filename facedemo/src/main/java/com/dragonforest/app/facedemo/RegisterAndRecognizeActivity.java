package com.dragonforest.app.facedemo;

import android.graphics.Point;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.dragonforest.app.facedemo.arcsoft.faceserver.CompareResult;
import com.dragonforest.app.facedemo.arcsoft.faceserver.FaceServer;
import com.dragonforest.app.facedemo.arcsoft.model.DrawInfo;
import com.dragonforest.app.facedemo.arcsoft.model.FacePreviewInfo;
import com.dragonforest.app.facedemo.arcsoft.util.ConfigUtil;
import com.dragonforest.app.facedemo.arcsoft.util.DrawHelper;
import com.dragonforest.app.facedemo.arcsoft.util.camera.CameraHelper;
import com.dragonforest.app.facedemo.arcsoft.util.camera.CameraListener;
import com.dragonforest.app.facedemo.arcsoft.util.face.FaceHelper;
import com.dragonforest.app.facedemo.arcsoft.util.face.FaceListener;
import com.dragonforest.app.facedemo.arcsoft.widget.FaceRectView;
import com.dragonforest.app.facedemo.dialog.RegisterDialog;
import com.dragonforest.app.facedemo.httpmock.ArcFaceSDK;
import com.dragonforest.app.facedemo.httpmock.PersonInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RegisterAndRecognizeActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * 相机预览控件
     */
    private View previewView;
    /**
     * 绘制人脸控件
     */
    private FaceRectView faceRectView;
    private Button btn_register;
    private Button btn_recognize;

    // 数据
    private FaceEngine faceEngine;
    private FaceHelper faceHelper;
    private int afCode = -1;
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当前人的信息
     */
    private PersonInfo personInfo;

    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    private static final float SIMILAR_THRESHOLD = 0.8F;

    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    //=================人脸注册状态===================================
    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    //=============人脸注册状态=======================================
    //=============人脸识别状态=======================================
    /**
     * 识别人脸状态码，准备识别
     */
    private static final int RECOGNIZE_STATUS_READY = 0;
    /**
     * 识别人脸状态码，识别中
     */
    private static final int RECOGNIZE_STATUS_PROCESSING = 1;
    /**
     * 识别人脸状态码，识别结束（无论成功失败）
     */
    private static final int RECOGNIZE_STATUS_DONE = 2;
    private int recognizeStatus = RECOGNIZE_STATUS_DONE;
    //=============人脸识别状态=======================================

    private int registerStatus = REGISTER_STATUS_DONE;

    /**
     * 注册人脸dialog
     */
    private RegisterDialog registerDialog;

    /**
     * 人脸消息
     */
    private String faceMsg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_and_recognize);
        initView();
    }

    private void initView() {
        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = findViewById(R.id.face_rect_view);
        btn_register = findViewById(R.id.btn_register);
        btn_recognize = findViewById(R.id.btn_recognize);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRecognize();
            }
        });

    }

    /**
     * 点击注册
     */
    private void clickRegister() {
        if (personInfo == null) {
            // 显示输入信息
            if (registerDialog == null) {
                registerDialog = new RegisterDialog(this);
                registerDialog.setCancelable(false);
                registerDialog.setOnRegisterListener(new RegisterDialog.OnRegisterListener() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onRegister(String name, String sex, int groupId) {
                        personInfo = new PersonInfo();
                        personInfo.setGroupId(groupId);
                        personInfo.setSex(sex);
                        personInfo.setName(name);
                        Toast.makeText(RegisterAndRecognizeActivity.this, "设置完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            registerDialog.show();
        } else {
            if (registerStatus == REGISTER_STATUS_DONE) {
                registerStatus = REGISTER_STATUS_READY;
            }
        }
    }

    /**
     * 点击识别
     */
    private void clickRecognize() {
        if (recognizeStatus == RECOGNIZE_STATUS_DONE) {
            recognizeStatus = RECOGNIZE_STATUS_READY;
            faceMsg = "";
            Toast.makeText(this, "开始识别", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i("TAG", "initEngine:  init: " + afCode + "  version:" + versionInfo);

        if (afCode != ErrorInfo.MOK) {
            Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {

        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Log.i("TAG", "unInitEngine: " + afCode);
        }
    }

    private void initCamera() {
        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e("TAG", "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId) {
                Log.e("TAG", "onFaceFeatureInfoGet(),thread:" + Thread.currentThread().getName());
                if (faceFeature != null) {
                    registerFace(faceFeature);
                    searchFace(faceFeature, requestId);
                }
            }

        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                Log.i("TAG", "onCameraOpened: " + drawHelper.toString());
                faceHelper = new FaceHelper.Builder()
                        .faceEngine(faceEngine)
                        .frThreadNum(MAX_DETECT_NUM)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .currentTrackId(ConfigUtil.getTrackId(RegisterAndRecognizeActivity.this.getApplicationContext()))
                        .build();
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        // 调用此会回调FaceListener#onFaceFeatureInfoGet()
                        faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i("TAG", "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i("TAG", "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i("TAG", "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    /**
     * 画方框 和 人信息
     *
     * @param facePreviewInfoList
     */
    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
//            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
//            Integer liveness = 0;
//            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
//                    liveness == null ? LivenessInfo.UNKNOWN : liveness,
//                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
                    0,
                    faceMsg));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    /**
     * 注册人脸
     *
     * @param feature
     */
    private void registerFace(final FaceFeature feature) {
        if (registerStatus == REGISTER_STATUS_READY) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            personInfo.setFaceFeature(feature);
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {
                    // TODO: 2019/8/6 服务器端注册人脸并返回
                    boolean success = ArcFaceSDK.getInstance().registerFace(personInfo);
                    emitter.onNext(success);
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
                            Toast.makeText(RegisterAndRecognizeActivity.this, result, Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(RegisterAndRecognizeActivity.this, "register failed!", Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {
                            Toast.makeText(RegisterAndRecognizeActivity.this, "register complete!", Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }
                    });
        }
    }

    /**
     * 查找人脸
     *
     * @param frFace
     * @param requestId
     */
    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        if (recognizeStatus == RECOGNIZE_STATUS_READY) {
            recognizeStatus = RECOGNIZE_STATUS_PROCESSING;
            Observable
                    .create(new ObservableOnSubscribe<CompareResult>() {
                        @Override
                        public void subscribe(ObservableEmitter<CompareResult> emitter) {
                            // TODO: 2019/8/5 服务器比对结果 并返回
                            CompareResult compareResult = ArcFaceSDK.getInstance().searchTopSimilarFace(frFace);
                            if (compareResult == null) {
                                emitter.onError(null);
                            } else {
                                emitter.onNext(compareResult);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CompareResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(CompareResult compareResult) {
                            recognizeStatus = RECOGNIZE_STATUS_DONE;
                            if (compareResult == null || compareResult.getUserName() == null) {
                                Toast.makeText(RegisterAndRecognizeActivity.this, "结束，compareResult == null", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                                Toast.makeText(RegisterAndRecognizeActivity.this, "结束,success,找到 " + compareResult.getUserName() + "", Toast.LENGTH_SHORT).show();
                                faceMsg = compareResult.getUserName();
                            } else {
                                Toast.makeText(RegisterAndRecognizeActivity.this, "结束,success,未找到 ", Toast.LENGTH_SHORT).show();
                                faceMsg = "未找到";
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            recognizeStatus = RECOGNIZE_STATUS_DONE;
                            Toast.makeText(RegisterAndRecognizeActivity.this, "结束：onError（）" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onComplete() {
                            recognizeStatus = RECOGNIZE_STATUS_DONE;
                            Toast.makeText(RegisterAndRecognizeActivity.this, "结束，onComplete（）", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onGlobalLayout() {
        initEngine();
        initCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(this, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        FaceServer.getInstance().unInit();
    }
}
